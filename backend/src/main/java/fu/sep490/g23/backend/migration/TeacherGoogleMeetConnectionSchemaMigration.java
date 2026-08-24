package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Order(500)
public class TeacherGoogleMeetConnectionSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        if (consolidated()) {
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS teacher_google_meet_connections (
                    id BIGSERIAL PRIMARY KEY,
                    teacher_id BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
                    google_subject VARCHAR(255) NOT NULL,
                    google_email VARCHAR(255) NOT NULL,
                    encrypted_refresh_token TEXT NOT NULL,
                    scopes VARCHAR(500) NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    connected_at TIMESTAMP NOT NULL,
                    last_used_at TIMESTAMP,
                    revoked_at TIMESTAMP,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT ck_teacher_google_meet_status CHECK (
                        status IN ('CONNECTED', 'REAUTH_REQUIRED', 'DISCONNECTED')
                    )
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_teacher_google_meet_status
                ON teacher_google_meet_connections(status)
                """);
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.auth_tokens') IS NOT NULL THEN
                        ALTER TABLE auth_tokens DROP CONSTRAINT IF EXISTS auth_tokens_type_check;
                        ALTER TABLE auth_tokens
                            ADD CONSTRAINT auth_tokens_type_check
                            CHECK (type IN ('EMAIL_VERIFICATION', 'PASSWORD_RESET', 'GOOGLE_MEET_CONNECTION'));
                    END IF;
                END $$;
                """);
        jdbcTemplate.execute("""
                ALTER TABLE classroom_offerings
                ADD COLUMN IF NOT EXISTS virtual_meeting_owner_id BIGINT
                """);
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint
                        WHERE conname = 'fk_classroom_virtual_meeting_owner'
                    ) THEN
                        ALTER TABLE classroom_offerings
                            ADD CONSTRAINT fk_classroom_virtual_meeting_owner
                            FOREIGN KEY (virtual_meeting_owner_id) REFERENCES users(id);
                    END IF;
                END $$;
                """);
        // Clear an outdated staff owner. The service will assign the teacher and recreate
        // the classroom's shared space on the next staff retry.
        jdbcTemplate.execute("""
                UPDATE classroom_offerings
                SET virtual_meeting_owner_id = NULL
                WHERE primary_teacher_id IS NOT NULL
                  AND virtual_meeting_owner_id IS DISTINCT FROM primary_teacher_id
                """);
        // Backfill existing classes after the move from a room-per-session model
        // to one shared Google Meet space per classroom.
        jdbcTemplate.execute("""
                UPDATE classroom_sessions target
                SET lark_meeting_id = source.lark_meeting_id,
                    lark_meeting_no = source.lark_meeting_no,
                    lark_meeting_url = source.lark_meeting_url,
                    lark_meeting_status = 'SCHEDULED',
                    lark_sync_status = 'SYNCED',
                    lark_sync_error = NULL,
                    lark_synced_at = CURRENT_TIMESTAMP,
                    recording_provider = source.recording_provider,
                    recording_sync_status = source.recording_sync_status,
                    recording_sync_error = source.recording_sync_error
                FROM (
                    SELECT DISTINCT ON (source_session.classroom_offering_id)
                           source_session.classroom_offering_id,
                           source_session.lark_meeting_id,
                           source_session.lark_meeting_no,
                           source_session.lark_meeting_url,
                           source_session.recording_provider,
                           source_session.recording_sync_status,
                           source_session.recording_sync_error
                    FROM classroom_sessions source_session
                    WHERE source_session.delivery_mode = 'VIRTUAL'
                      AND source_session.lark_meeting_id LIKE 'spaces/%'
                      AND source_session.lark_meeting_url LIKE 'https://meet.google.com/%'
                    ORDER BY source_session.classroom_offering_id,
                             source_session.session_date ASC,
                             source_session.start_time ASC,
                             source_session.id ASC
                ) source
                WHERE target.delivery_mode = 'VIRTUAL'
                  AND target.classroom_offering_id = source.classroom_offering_id
                  AND target.status NOT IN ('COMPLETED', 'CANCELLED')
                  AND (
                      target.lark_meeting_id IS DISTINCT FROM source.lark_meeting_id
                      OR target.lark_sync_status IS DISTINCT FROM 'SYNCED'
                      OR target.lark_sync_error IS NOT NULL
                  )
                """);
        // A shared Google Meet space has one conference record per actual lesson. Earlier
        // versions selected the first record for every session sharing that space. Clear
        // only duplicated Google recordings so the corrected time-based sync can reassign
        // the file to its matching scheduled session.
        jdbcTemplate.execute("""
                UPDATE classroom_sessions target
                SET recording_url = NULL,
                    recording_visible = FALSE,
                    recording_duration_ms = NULL,
                    recording_sync_status = 'PROCESSING',
                    recording_synced_at = NULL,
                    recording_last_attempt_at = NULL,
                    recording_sync_error = NULL,
                    recording_sync_attempts = 0,
                    recording_published_at = NULL,
                    recording_expires_at = NULL
                FROM (
                    SELECT lark_meeting_id, recording_url
                    FROM classroom_sessions
                    WHERE lark_meeting_id LIKE 'spaces/%'
                      AND recording_provider = 'GOOGLE_MEET'
                      AND recording_url IS NOT NULL
                    GROUP BY lark_meeting_id, recording_url
                    HAVING COUNT(*) > 1
                ) duplicated
                WHERE target.lark_meeting_id = duplicated.lark_meeting_id
                  AND target.recording_url = duplicated.recording_url
                  AND target.recording_provider = 'GOOGLE_MEET'
                """);
    }

    private boolean consolidated() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.user_auxiliary_records') IS NOT NULL",
                Boolean.class
        ));
    }
}
