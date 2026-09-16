# EnglishLab Master Demo Dataset

Single source of truth for center-scale demo data covering `2026-06-01` → `2026-10-31` with fixed reference date `2026-09-16`.

## Priority

`DATA QUALITY > BUSINESS COVERAGE > RECORD COUNT`

## Generate workbook + import JSON

```bash
node ops/sheet-data/master/generate.mjs
```

Outputs:

- `ops/sheet-data/EnglishLab-Master-SheetData.xlsx`
- `backend/src/main/resources/seed/master/master-dataset.json`
- `ops/sheet-data/master-data-validation-report.json`

Seed: `DEMO_DATA_SEED=20260916` (deterministic).

## Enable import (local only)

```properties
APP_SEED_MASTER_ENABLED=true
```

Default is **false**. Production is unchanged unless the flag is explicitly enabled.

Optional:

```properties
APP_SEED_MASTER_CLEANUP_BEFORE_IMPORT=true
APP_SEED_MASTER_DATASET_CLASSPATH=seed/master/master-dataset.json
```

## Protected / preserved

Never owned by MASTER:

- Placement Test / Mock Test content banks
- `e2-ielts-practice-tests`
- `ielts-master-vocabulary-band-7-plus`
- Learner `0386852628z@gmail.com` (progress KEEP AS-IS)
- Teacher `alien1062004@gmail.com` (profile / Meet KEEP AS-IS)

## Cleanup

Cleanup deletes only:

- `demo.*@englishlab.local` users
- `demo-class-*`, `demo-room-*`, `demo-*` online courses, `DEMO_*` programs/discounts
- Related MASTER transactional rows

It never deletes preserved Gmail accounts, protected course content, or Flyway history.

## Legacy seeders

When MASTER is enabled, legacy demo/sheet/showcase seeders are skipped via `MasterSeedGate`.
Protected base seeders (E2 / Vocabulary / TOEIC mock bank / rubrics) still run if content is missing.
