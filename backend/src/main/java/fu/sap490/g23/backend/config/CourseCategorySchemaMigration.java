package fu.sap490.g23.backend.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseCategorySchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void relaxCategoryCodeConstraint() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.course_categories') IS NOT NULL THEN
                        ALTER TABLE course_categories
                            DROP CONSTRAINT IF EXISTS course_categories_code_check;

                        IF NOT EXISTS (
                            SELECT 1
                            FROM pg_constraint
                            WHERE conname = 'course_categories_code_format_check'
                        ) THEN
                            ALTER TABLE course_categories
                                ADD CONSTRAINT course_categories_code_format_check
                                CHECK (code ~ '^[A-Z][A-Z0-9_]*$');
                        END IF;
                    END IF;
                END $$;
                """);
    }
}
