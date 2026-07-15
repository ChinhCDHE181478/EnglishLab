package fu.sap490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrainingProgramMaterialCleanupMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.classroom_materials') IS NOT NULL THEN
                        DELETE FROM classroom_materials
                        WHERE UPPER(COALESCE(source_type, '')) = 'PROGRAM_LIBRARY';
                    END IF;

                    IF to_regclass('public.training_program_materials') IS NOT NULL THEN
                        DELETE FROM training_program_materials;
                    END IF;
                END $$;
                """);
    }
}
