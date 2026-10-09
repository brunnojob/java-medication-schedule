import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public final class MedicationSchedule {
    record Dose(Instant scheduled, String status, Instant recorded) {}
    record Plan(ZoneId zone, List<LocalTime> times, Map<Instant, Dose> records) {}
    static Plan read(Path path) throws Exception {
        List<String> lines = Files.readAllLines(path);
        if (lines.isEmpty()) throw new IllegalArgumentException("schedule not initialized");
        String[] header = lines.get(0).split("\\|", -1);
        if (header.length != 3 || !header[0].equals("v1")) throw new IllegalArgumentException("invalid plan");
        ZoneId zone = ZoneId.of(header[1]); List<LocalTime> times = parseTimes(header[2]);
        var records = new LinkedHashMap<Instant, Dose>();
        for (String line : lines.subList(1, lines.size())) {
            String[] f = line.split("\\|", -1);
            if (f.length != 3 || !Set.of("taken", "skipped").contains(f[1])) throw new IllegalArgumentException("invalid dose event");
            Dose dose = new Dose(Instant.parse(f[0]), f[1], Instant.parse(f[2]));
            Dose prior = records.putIfAbsent(dose.scheduled(), dose);
            if (prior != null && !prior.equals(dose)) throw new IllegalStateException("conflicting dose event");
        }
        return new Plan(zone, times, records);
    }
    static List<LocalTime> parseTimes(String source) {
        var values = new TreeSet<LocalTime>();
        for (String value : source.split(",", -1)) {
            LocalTime time = LocalTime.parse(value);
            if (time.getSecond() != 0 || time.getNano() != 0 || !values.add(time)) throw new IllegalArgumentException("unique minute-resolution times required");
        }
        if (values.isEmpty() || values.size() > 24) throw new IllegalArgumentException("one to 24 times required");
        return List.copyOf(values);
    }
    static List<Instant> scheduled(Plan plan, LocalDate date) {
        var result = new ArrayList<Instant>();
        for (LocalTime time : plan.times()) {
            LocalDateTime local = date.atTime(time);
            var offsets = plan.zone().getRules().getValidOffsets(local);
            if (!offsets.isEmpty()) result.add(local.atOffset(offsets.get(0)).toInstant());
        }
        return result;
    }
    static void append(Path path, String row) throws Exception {
        try (FileChannel channel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
            ByteBuffer buffer = ByteBuffer.wrap((row + "\n").getBytes(StandardCharsets.UTF_8));
            while (buffer.hasRemaining()) channel.write(buffer); channel.force(true);
        }
    }
    static void report(Plan plan, LocalDate date, Instant now) {
        System.out.print("{\"date\":\"" + date + "\",\"zone\":\"" + plan.zone() + "\",\"doses\":[");
        var doses = scheduled(plan, date);
        for (int i = 0; i < doses.size(); i++) {
            Instant scheduled = doses.get(i); Dose record = plan.records().get(scheduled);
            String status = record == null ? scheduled.isBefore(now) ? "unrecorded" : "pending" : record.status();
            if (i > 0) System.out.print(",");
            System.out.print("{\"scheduled\":\"" + scheduled + "\",\"status\":\"" + status + "\"}");
        }
        System.out.println("]}");
    }
    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("MedicationSchedule file init zone HH:mm,HH:mm | report YYYY-MM-DD | record instant taken|skipped");
        Path file = Path.of(args[0]);
        try (FileChannel channel = FileChannel.open(file.resolveSibling(file.getFileName() + ".lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
             var lock = channel.lock()) {
            if (!lock.isValid()) throw new IllegalStateException("file lock unavailable");
            if (args[1].equals("init")) {
                if (args.length != 4 || Files.exists(file)) throw new IllegalStateException("new schedule and zone/times required");
                ZoneId zone = ZoneId.of(args[2]); var times = parseTimes(args[3]);
                append(file, "v1|" + zone + "|" + String.join(",", times.stream().map(LocalTime::toString).toList())); return;
            }
            Plan plan = read(file);
            if (args[1].equals("report") && args.length == 3) { report(plan, LocalDate.parse(args[2]), Instant.now()); return; }
            if (!args[1].equals("record") || args.length != 4 || !Set.of("taken", "skipped").contains(args[3])) throw new IllegalArgumentException("invalid command");
            Instant scheduled = Instant.parse(args[2]), now = Instant.now();
            LocalDate date = scheduled.atZone(plan.zone()).toLocalDate();
            if (!scheduled(plan, date).contains(scheduled)) throw new IllegalArgumentException("timestamp is not in the schedule");
            if (scheduled.isAfter(now)) throw new IllegalArgumentException("cannot record a future scheduled event");
            Dose prior = plan.records().get(scheduled);
            if (prior != null) { if (!prior.status().equals(args[3])) throw new IllegalStateException("dose already recorded"); }
            else append(file, scheduled + "|" + args[3] + "|" + now);
            report(read(file), date, now);
        }
    }
}
