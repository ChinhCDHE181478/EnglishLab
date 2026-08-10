package fu.sap490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminBroadcastSchemaMigration {
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS admin_broadcasts (
                    id BIGSERIAL PRIMARY KEY,
                    title VARCHAR(180) NOT NULL,
                    message VARCHAR(4000) NOT NULL,
                    target_role VARCHAR(30),
                    action_path VARCHAR(500),
                    send_in_app BOOLEAN NOT NULL DEFAULT TRUE,
                    send_email BOOLEAN NOT NULL DEFAULT FALSE,
                    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                    scheduled_at TIMESTAMP,
                    sent_at TIMESTAMP,
                    recipient_count INTEGER NOT NULL DEFAULT 0,
                    in_app_success_count INTEGER NOT NULL DEFAULT 0,
                    email_queued_count INTEGER NOT NULL DEFAULT 0,
                    failure_reason VARCHAR(1000),
                    created_by BIGINT NOT NULL REFERENCES users(id),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                CREATE INDEX IF NOT EXISTS idx_admin_broadcast_status_schedule
                    ON admin_broadcasts(status, scheduled_at);
                """);
    }
}
