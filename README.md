# Medication Schedule

Agenda de horários informados pelo usuário, com fuso horário, tratamento de mudanças de horário e histórico de registros.

## Executar

Requisitos: Java 17.

```sh
javac MedicationSchedule.java
java MedicationSchedule agenda.log init America/Sao_Paulo 08:00,20:00
java MedicationSchedule agenda.log report 2026-10-09 > resultado.json
```

## Funcionamento

Comando `record instante taken|skipped` registra um horário existente. Eventos futuros e decisões conflitantes são recusados. Horários inexistentes em mudanças de fuso são omitidos; horários duplicados usam a primeira ocorrência. O programa não recomenda doses nem conduta para atraso.

## Persistência de resultados

O arquivo de operações está em [vercel-home-telemetry-api.vercel.app](https://vercel-home-telemetry-api.vercel.app/laboratory.html?project=java-medication-schedule). As migrações Supabase estão no [repositório da API](https://github.com/brunnojob/vercel-home-telemetry-api/tree/main/supabase/migrations).

```sh
python cloud/sync.py enqueue resultado.json --project java-medication-schedule
python cloud/sync.py sync
```

Defina `BRUNNODEV_ACCESS_TOKEN` com sua sessão. A fila SQLite conserva os relatórios até confirmação do servidor; o mesmo conteúdo não gera registros duplicados. Tokens não são gravados no código.
