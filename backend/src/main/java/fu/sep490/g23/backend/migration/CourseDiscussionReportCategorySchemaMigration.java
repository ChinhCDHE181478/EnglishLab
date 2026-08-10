package fu.sap490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseDiscussionReportCategorySchemaMigration {
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureReasonCategoryColumn() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.course_discussion_reports') IS NOT NULL THEN
                        ALTER TABLE course_discussion_reports
                            ADD COLUMN IF NOT EXISTS reason_category VARCHAR(30) NOT NULL DEFAULT 'OTHER';
                    END IF;
                END $$;
                """);
    }
}
