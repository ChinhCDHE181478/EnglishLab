package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(500)
public class EnrollmentRequestSchemaMigration implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (consolidated()) {
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS course_enrollment_requests (
                    id BIGSERIAL PRIMARY KEY,
                    learner_id BIGINT NOT NULL REFERENCES users(id),
                    course_offering_id BIGINT REFERENCES training_programs(id),
                    requested_classroom_id BIGINT REFERENCES classroom_offerings(id),
                    placement_attempt_id BIGINT REFERENCES placement_test_attempts(id),
                    assigned_classroom_id BIGINT REFERENCES classroom_offerings(id),
                    contact_name VARCHAR(100),
                    contact_email VARCHAR(150),
                    contact_phone VARCHAR(30),
                    facebook_url VARCHAR(500),
                    desired_class_code VARCHAR(120),
                    consultation_track VARCHAR(80),
                    study_work_goal VARCHAR(500),
                    status VARCHAR(40) NOT NULL DEFAULT 'SUBMITTED',
                    request_source VARCHAR(20) NOT NULL DEFAULT 'ONLINE',
                    confirmed_level VARCHAR(30),
                    preferred_schedule VARCHAR(500),
                    campus_preference VARCHAR(255),
                    learner_note VARCHAR(700),
                    staff_note VARCHAR(700),
                    rejection_reason VARCHAR(700),
                    reviewed_by_id BIGINT REFERENCES users(id),
                    reviewed_at TIMESTAMP,
                    cancelled_at TIMESTAMP,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    version BIGINT NOT NULL DEFAULT 0
                );

                CREATE TABLE IF NOT EXISTS course_enrollment_request_history (
                    id BIGSERIAL PRIMARY KEY,
                    enrollment_request_id BIGINT NOT NULL REFERENCES course_enrollment_requests(id),
                    from_status VARCHAR(40),
                    to_status VARCHAR(40) NOT NULL,
                    actor_id BIGINT REFERENCES users(id),
                    reason VARCHAR(700),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                );

                CREATE INDEX IF NOT EXISTS idx_enrollment_request_learner_created
                    ON course_enrollment_requests(learner_id, created_at DESC);
                CREATE INDEX IF NOT EXISTS idx_enrollment_request_status_created
                    ON course_enrollment_requests(status, created_at);
                CREATE INDEX IF NOT EXISTS idx_enrollment_request_pool
                    ON course_enrollment_requests(course_offering_id, confirmed_level, preferred_schedule)
                    WHERE status = 'WAITING_FOR_CLASS';
                CREATE INDEX IF NOT EXISTS idx_enrollment_request_history_request
                    ON course_enrollment_request_history(enrollment_request_id, created_at, id);

                ALTER TABLE course_enrollment_requests
                    ADD COLUMN IF NOT EXISTS requested_classroom_id BIGINT REFERENCES classroom_offerings(id);
                ALTER TABLE course_enrollment_requests
                    ADD COLUMN IF NOT EXISTS request_source VARCHAR(20) NOT NULL DEFAULT 'ONLINE';
                ALTER TABLE course_enrollment_requests
                    DROP CONSTRAINT IF EXISTS course_enrollment_requests_source_check;
                ALTER TABLE course_enrollment_requests
                    ADD CONSTRAINT course_enrollment_requests_source_check
                    CHECK (request_source IN ('ONLINE', 'CENTER'));
                ALTER TABLE course_enrollment_requests
                    ALTER COLUMN course_offering_id DROP NOT NULL;
                ALTER TABLE course_enrollment_requests
                    ADD COLUMN IF NOT EXISTS contact_name VARCHAR(100),
                    ADD COLUMN IF NOT EXISTS contact_email VARCHAR(150),
                    ADD COLUMN IF NOT EXISTS contact_phone VARCHAR(30),
                    ADD COLUMN IF NOT EXISTS facebook_url VARCHAR(500),
                    ADD COLUMN IF NOT EXISTS desired_class_code VARCHAR(120),
                    ADD COLUMN IF NOT EXISTS consultation_track VARCHAR(80),
                    ADD COLUMN IF NOT EXISTS study_work_goal VARCHAR(500);
                CREATE INDEX IF NOT EXISTS idx_enrollment_request_requested_classroom
                    ON course_enrollment_requests(requested_classroom_id, status);

                ALTER TABLE course_enrollment_requests
                    DROP CONSTRAINT IF EXISTS course_enrollment_requests_status_check;
                ALTER TABLE course_enrollment_requests
                    ADD CONSTRAINT course_enrollment_requests_status_check
                    CHECK (status IN (
                        'SUBMITTED', 'INVITATION_SENT', 'TEST_SCHEDULED',
                        'AWAITING_PLACEMENT_TEST', 'PLACEMENT_TEST_COMPLETED',
                        'UNDER_STAFF_REVIEW', 'WAITING_FOR_CLASS', 'CLASS_PROPOSED',
                        'CLASS_ASSIGNED', 'REJECTED', 'CANCELLED'
                    ));

                ALTER TABLE course_enrollment_request_history
                    DROP CONSTRAINT IF EXISTS course_enrollment_request_history_from_status_check;
                ALTER TABLE course_enrollment_request_history
                    ADD CONSTRAINT course_enrollment_request_history_from_status_check
                    CHECK (from_status IS NULL OR from_status IN (
                        'SUBMITTED', 'INVITATION_SENT', 'TEST_SCHEDULED',
                        'AWAITING_PLACEMENT_TEST', 'PLACEMENT_TEST_COMPLETED',
                        'UNDER_STAFF_REVIEW', 'WAITING_FOR_CLASS', 'CLASS_PROPOSED',
                        'CLASS_ASSIGNED', 'REJECTED', 'CANCELLED'
                    ));

                ALTER TABLE course_enrollment_request_history
                    DROP CONSTRAINT IF EXISTS course_enrollment_request_history_to_status_check;
                ALTER TABLE course_enrollment_request_history
                    ADD CONSTRAINT course_enrollment_request_history_to_status_check
                    CHECK (to_status IN (
                        'SUBMITTED', 'INVITATION_SENT', 'TEST_SCHEDULED',
                        'AWAITING_PLACEMENT_TEST', 'PLACEMENT_TEST_COMPLETED',
                        'UNDER_STAFF_REVIEW', 'WAITING_FOR_CLASS', 'CLASS_PROPOSED',
                        'CLASS_ASSIGNED', 'REJECTED', 'CANCELLED'
                    ));
                """);
    }

    private boolean consolidated() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.classroom_operation_records') IS NOT NULL",
                Boolean.class
        ));
    }
}
