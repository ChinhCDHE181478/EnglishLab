package fu.sep490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component @RequiredArgsConstructor
public class AuditLogSchemaMigration {
    private final JdbcTemplate jdbcTemplate;
    @PostConstruct public void ensureTable(){jdbcTemplate.execute("""
        CREATE TABLE IF NOT EXISTS system_audit_logs (
          id BIGSERIAL PRIMARY KEY, actor_email VARCHAR(150) NOT NULL, action VARCHAR(100) NOT NULL,
          target_type VARCHAR(100), target_id VARCHAR(100), detail VARCHAR(1000), created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        );
        CREATE INDEX IF NOT EXISTS idx_audit_logs_created_at ON system_audit_logs(created_at DESC);
        CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_action ON system_audit_logs(actor_email, action);
        """);}
}
