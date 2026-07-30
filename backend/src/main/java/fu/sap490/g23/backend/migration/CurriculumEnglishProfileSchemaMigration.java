package fu.sap490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CurriculumEnglishProfileSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureEnglishProfileColumns() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.curriculum_programs') IS NOT NULL THEN
                        ALTER TABLE curriculum_programs
                            ADD COLUMN IF NOT EXISTS program_track VARCHAR(60);

                        ALTER TABLE curriculum_programs
                            ADD COLUMN IF NOT EXISTS focus_skills VARCHAR(240);

                        UPDATE curriculum_programs
                        SET exam_category = 'GENERAL_ENGLISH'
                        WHERE exam_category IN ('GENERAL', 'COMMUNICATION', 'FOUNDATION');
                    END IF;
                END $$;
                """);
    }
}
