# Medication Schedule

A schedule of user-supplied times, with time zones, clock-change handling, and a record history.

## Run

Requirements: Java 17.

```sh
javac MedicationSchedule.java
java MedicationSchedule agenda.log init America/Sao_Paulo 08:00,20:00
java MedicationSchedule agenda.log report 2026-10-09 > result.json
```

## Behavior

`record instant taken|skipped` records an existing scheduled time. Future events and conflicting decisions are rejected. Nonexistent times during clock changes are omitted; duplicated times use the first occurrence. The program does not recommend doses or actions for missed times.

## Optional report archive

Use the [shared operations archive client](https://github.com/brunnojob/vercel-home-telemetry-api/tree/main/cloud) to queue `result.json` under project `java-medication-schedule`. The client uses `BRUNNODEV_ACCESS_TOKEN` and retains unacknowledged reports locally.

## License

Original source and documentation are MIT licensed; see [LICENSE](LICENSE). Third-party dependencies and media retain their respective terms. Maintained by [Brunno Dev](https://brunnodev.store).
