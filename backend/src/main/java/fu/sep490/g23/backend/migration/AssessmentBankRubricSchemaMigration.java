package fu.sep490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AssessmentBankRubricSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureAssessmentBankRubricColumn() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.assessment_bank_items') IS NOT NULL THEN
                        ALTER TABLE assessment_bank_items
                            ADD COLUMN IF NOT EXISTS rubric_id BIGINT;

                        IF to_regclass('public.assessment_rubrics') IS NOT NULL
                           AND NOT EXISTS (
                                SELECT 1
                                FROM pg_constraint
                                WHERE conname = 'fk_assessment_bank_items_rubric'
                           ) THEN
                            ALTER TABLE assessment_bank_items
                                ADD CONSTRAINT fk_assessment_bank_items_rubric
                                FOREIGN KEY (rubric_id) REFERENCES assessment_rubrics(id);
                        END IF;
                    END IF;
                END $$;
                """);
    }
}
