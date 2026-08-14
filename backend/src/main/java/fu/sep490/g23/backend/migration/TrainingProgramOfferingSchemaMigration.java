package fu.sep490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Ensures training_programs capacity/schedule columns exist before seeders insert rows.
 * Also heals legacy {@code default_capacity} (NOT NULL, no default) so Hibernate inserts
 * that only populate {@code max_capacity} do not fail.
 */
@Component
@RequiredArgsConstructor
public class TrainingProgramOfferingSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureTrainingProgramOfferingColumns() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.training_programs') IS NULL THEN
                        RETURN;
                    END IF;

                    ALTER TABLE training_programs
                        ADD COLUMN IF NOT EXISTS max_capacity INTEGER NOT NULL DEFAULT 30,
                        ADD COLUMN IF NOT EXISTS planned_start_date DATE,
                        ADD COLUMN IF NOT EXISTS planned_schedule VARCHAR(500);

                    UPDATE training_programs SET max_capacity = 30 WHERE max_capacity IS NULL;
                    ALTER TABLE training_programs ALTER COLUMN max_capacity SET DEFAULT 30;
                    ALTER TABLE training_programs ALTER COLUMN max_capacity SET NOT NULL;

                    IF EXISTS (
                        SELECT 1
                        FROM information_schema.columns
                        WHERE table_schema = 'public'
                          AND table_name = 'training_programs'
                          AND column_name = 'default_capacity'
                    ) THEN
                        UPDATE training_programs
                        SET default_capacity = COALESCE(default_capacity, max_capacity, 30)
                        WHERE default_capacity IS NULL;
                        ALTER TABLE training_programs ALTER COLUMN default_capacity SET DEFAULT 30;
                        ALTER TABLE training_programs ALTER COLUMN default_capacity SET NOT NULL;
                    END IF;
                END $$;
                """);
    }
}
