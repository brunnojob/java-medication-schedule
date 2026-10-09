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

## Result synchronization

The [operations archive](https://vercel-home-telemetry-api.vercel.app/laboratory.html?project=java-medication-schedule) stores execution results. Supabase migrations are in the [API repository](https://github.com/brunnojob/vercel-home-telemetry-api/tree/main/supabase/migrations).

```sh
python cloud/sync.py enqueue result.json --project java-medication-schedule
python cloud/sync.py sync
```

Set `BRUNNODEV_ACCESS_TOKEN` to your session token. The SQLite outbox retains reports until the server confirms persistence; identical content does not create duplicate records. Tokens are not stored in source code. To run the synchronization tests:

```sh
python -m unittest discover -s cloud
```
