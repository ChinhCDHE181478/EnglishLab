package fu.sep490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClassroomWaitlistSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureWaitlistPriorityColumn() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.classroom_enrollments') IS NOT NULL THEN
                        ALTER TABLE classroom_enrollments
                            ADD COLUMN IF NOT EXISTS waitlist_priority INTEGER;

                        WITH ranked AS (
                            SELECT id,
                                   ROW_NUMBER() OVER (
                                       PARTITION BY classroom_offering_id
                                       ORDER BY waitlist_priority NULLS LAST, enrolled_at, id
                                   ) AS queue_position
                            FROM classroom_enrollments
                            WHERE registration_status = 'WAITLIST'
                        )
                        UPDATE classroom_enrollments enrollment
                        SET waitlist_priority = ranked.queue_position
                        FROM ranked
                        WHERE enrollment.id = ranked.id;

                        UPDATE classroom_enrollments
                        SET waitlist_priority = NULL
                        WHERE registration_status <> 'WAITLIST'
                          AND waitlist_priority IS NOT NULL;

                        CREATE INDEX IF NOT EXISTS idx_classroom_enrollment_waitlist_order
                            ON classroom_enrollments (
                                classroom_offering_id,
                                registration_status,
                                waitlist_priority
                            );
                    END IF;
                END $$;
                """);
    }
}
