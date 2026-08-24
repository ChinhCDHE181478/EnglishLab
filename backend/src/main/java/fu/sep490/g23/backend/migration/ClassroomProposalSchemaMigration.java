package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(500)
public class ClassroomProposalSchemaMigration implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (consolidated()) {
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS classroom_proposals (
                    id BIGSERIAL PRIMARY KEY,
                    proposal_code VARCHAR(40) NOT NULL UNIQUE,
                    title VARCHAR(180) NOT NULL,
                    course_offering_id BIGINT NOT NULL REFERENCES training_programs(id),
                    delivery_type VARCHAR(20) NOT NULL,
                    placement_level VARCHAR(30),
                    capacity INTEGER NOT NULL,
                    planned_start_date DATE NOT NULL,
                    planned_end_date DATE NOT NULL,
                    schedule_weekdays VARCHAR(100) NOT NULL,
                    session_start_time TIME NOT NULL,
                    session_end_time TIME NOT NULL,
                    primary_teacher_id BIGINT REFERENCES users(id),
                    room_id BIGINT REFERENCES classroom_rooms(id),
                    offline_address VARCHAR(500),
                    virtual_meeting_url VARCHAR(700),
                    staff_note VARCHAR(700),
                    approval_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                    created_by_id BIGINT NOT NULL REFERENCES users(id),
                    submitted_by_id BIGINT REFERENCES users(id),
                    submitted_at TIMESTAMP,
                    reviewed_by_id BIGINT REFERENCES users(id),
                    reviewed_at TIMESTAMP,
                    review_note VARCHAR(700),
                    approved_classroom_id BIGINT REFERENCES classroom_offerings(id),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    version BIGINT NOT NULL DEFAULT 0
                );

                CREATE TABLE IF NOT EXISTS classroom_proposal_members (
                    id BIGSERIAL PRIMARY KEY,
                    proposal_id BIGINT NOT NULL REFERENCES classroom_proposals(id),
                    enrollment_request_id BIGINT NOT NULL REFERENCES course_enrollment_requests(id),
                    classroom_enrollment_id BIGINT,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_classroom_proposal_member UNIQUE (proposal_id, enrollment_request_id)
                );

                CREATE INDEX IF NOT EXISTS idx_classroom_proposal_status_created
                    ON classroom_proposals(approval_status, created_at);
                CREATE INDEX IF NOT EXISTS idx_classroom_proposal_member_request
                    ON classroom_proposal_members(enrollment_request_id);

                ALTER TABLE classroom_proposals
                    ALTER COLUMN placement_level DROP NOT NULL;
                """);
    }

    private boolean consolidated() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.classroom_operation_records') IS NOT NULL",
                Boolean.class
        ));
    }
}
