# Flyway migrations (EnglishLab Phase 3)

## Adoption modes

| Database state | `FLYWAY_BASELINE_ON_MIGRATE` | What happens |
|---|---|---|
| **Legacy pre-Flyway, non-empty**, no `flyway_schema_history` | `true` **once** | Flyway baselines at version 1 without re-running `V1__legacy_baseline.sql`, then applies V2+ |
| **Already Flyway-managed** (`flyway_schema_history` present) | `false` | Normal migrate/validate; do not re-baseline |
| **New empty database** | `false` (default) | Flyway applies `V1` then later versions from scratch |

After a legacy DB has been adopted, set `FLYWAY_BASELINE_ON_MIGRATE=false` permanently.

## Verification hygiene

Stale `target/classes/db/migration` can retain deleted SQL. Always run migration-sensitive checks with:

```bash
mvn clean test
# or
mvn clean spring-boot:run
```

Confirm runtime classpath contains only intended `V*` scripts under `target/classes/db/migration`.

## Slice 0 ledger

`application_schema_migrations` is retired. Schema ownership is `flyway_schema_history` only.
