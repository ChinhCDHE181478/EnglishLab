package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrainingProgramOfferingSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                ALTER TABLE training_programs
                    ADD COLUMN IF NOT EXISTS max_capacity INTEGER NOT NULL DEFAULT 30,
                    ADD COLUMN IF NOT EXISTS planned_start_date DATE,
                    ADD COLUMN IF NOT EXISTS planned_schedule VARCHAR(500);
                UPDATE training_programs SET max_capacity = 30 WHERE max_capacity IS NULL;
                ALTER TABLE training_programs ALTER COLUMN max_capacity SET DEFAULT 30;
                ALTER TABLE training_programs ALTER COLUMN max_capacity SET NOT NULL;
                """);
    }
}
