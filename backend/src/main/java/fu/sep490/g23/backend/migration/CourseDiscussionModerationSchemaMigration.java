package fu.sap490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseDiscussionModerationSchemaMigration {
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureModerationColumns() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.course_discussion_reports') IS NOT NULL THEN
                        ALTER TABLE course_discussion_reports
                            ADD COLUMN IF NOT EXISTS status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                            ADD COLUMN IF NOT EXISTS reviewed_by_id BIGINT,
                            ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP,
                            ADD COLUMN IF NOT EXISTS action_note VARCHAR(500);

                        IF to_regclass('public.users') IS NOT NULL
                           AND NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_discussion_reports_reviewer') THEN
                            ALTER TABLE course_discussion_reports
                                ADD CONSTRAINT fk_discussion_reports_reviewer
                                FOREIGN KEY (reviewed_by_id) REFERENCES users(id);
                        END IF;
                    END IF;
                END $$;
                """);
    }
}
