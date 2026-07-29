package fu.sap490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BackupSchemaMigration {
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureSchema() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS system_backup_records (
                    id BIGSERIAL PRIMARY KEY,
                    file_name VARCHAR(220) NOT NULL UNIQUE,
                    status VARCHAR(20) NOT NULL,
                    file_size_bytes BIGINT NOT NULL DEFAULT 0,
                    sha256 VARCHAR(64),
                    created_by VARCHAR(150) NOT NULL,
                    restored_by VARCHAR(150),
                    restored_at TIMESTAMP,
                    failure_reason VARCHAR(1000),
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                CREATE INDEX IF NOT EXISTS idx_system_backup_created_at
                    ON system_backup_records(created_at DESC);
                """);
    }
}
