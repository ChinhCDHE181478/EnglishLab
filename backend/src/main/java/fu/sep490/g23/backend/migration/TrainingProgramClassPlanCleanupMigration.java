package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Removes class-planning data that was previously stored on course products. */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 250)
@RequiredArgsConstructor
public class TrainingProgramClassPlanCleanupMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.training_programs') IS NULL THEN
                        RETURN;
                    END IF;

                    ALTER TABLE training_programs
                        DROP COLUMN IF EXISTS planned_start_date,
                        DROP COLUMN IF EXISTS planned_schedule,
                        DROP COLUMN IF EXISTS max_capacity,
                        DROP COLUMN IF EXISTS default_capacity;
                END $$;
                """);
    }
}
