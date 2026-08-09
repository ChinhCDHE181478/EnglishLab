package fu.sap490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TeacherGoogleMeetConnectionSchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
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
    }
}
