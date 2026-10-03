import java.time.*;
import java.time.format.DateTimeParseException;

public class MedicationSchedule {
    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("usage: java MedicationSchedule <last-dose-ISO-8601> <interval-hours> <now-ISO-8601>");
            System.exit(2);
        }
        try {
            Instant last = Instant.parse(args[0]);
            long hours = Long.parseLong(args[1]);
            Instant now = Instant.parse(args[2]);
            if (hours < 1 || hours > 168) throw new IllegalArgumentException("interval must be 1..168 hours");
            Instant due = last.plus(Duration.ofHours(hours));
            Duration delta = Duration.between(now, due);
            if (delta.isNegative()) System.out.printf("MISSED by %d minutes%n", Math.abs(delta.toMinutes()));
            else if (delta.isZero()) System.out.println("DUE NOW");
            else System.out.printf("NEXT DOSE IN %d hours %d minutes%n", delta.toHours(), delta.toMinutesPart());
            System.out.println("scheduled_at=" + due);
        } catch (DateTimeParseException | NumberFormatException ex) {
            System.err.println("invalid timestamp or interval");
            System.exit(2);
        }
    }
}