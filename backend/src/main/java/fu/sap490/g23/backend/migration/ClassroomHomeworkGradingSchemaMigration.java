package fu.sap490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClassroomHomeworkGradingSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureHomeworkGradingColumns() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.classroom_homework') IS NOT NULL THEN
                        ALTER TABLE classroom_homework
                            ADD COLUMN IF NOT EXISTS grading_mode VARCHAR(20) NOT NULL DEFAULT 'TEACHER';

                        ALTER TABLE classroom_homework
                            ADD COLUMN IF NOT EXISTS skill VARCHAR(30);

                        ALTER TABLE classroom_homework
                            ADD COLUMN IF NOT EXISTS rubric_id BIGINT;

                        IF to_regclass('public.assessment_rubrics') IS NOT NULL
                           AND NOT EXISTS (
                                SELECT 1
                                FROM pg_constraint
                                WHERE conname = 'fk_classroom_homework_rubric'
                           ) THEN
                            ALTER TABLE classroom_homework
                                ADD CONSTRAINT fk_classroom_homework_rubric
                                FOREIGN KEY (rubric_id) REFERENCES assessment_rubrics(id);
                        END IF;
                    END IF;
                END $$;
                """);
    }
}
