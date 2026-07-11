package fu.sap490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClassroomRecordingSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureRecordingColumns() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.classroom_sessions') IS NOT NULL THEN
                        ALTER TABLE classroom_sessions ADD COLUMN IF NOT EXISTS lark_reserve_id VARCHAR(255);
                        ALTER TABLE classroom_sessions ADD COLUMN IF NOT EXISTS recording_sync_status VARCHAR(30) NOT NULL DEFAULT 'NOT_AVAILABLE';
                        ALTER TABLE classroom_sessions ADD COLUMN IF NOT EXISTS recording_provider VARCHAR(30);
                        ALTER TABLE classroom_sessions ADD COLUMN IF NOT EXISTS recording_duration_ms BIGINT;
                        ALTER TABLE classroom_sessions ADD COLUMN IF NOT EXISTS recording_synced_at TIMESTAMP;
                        ALTER TABLE classroom_sessions ADD COLUMN IF NOT EXISTS recording_last_attempt_at TIMESTAMP;
                        ALTER TABLE classroom_sessions ADD COLUMN IF NOT EXISTS recording_sync_error VARCHAR(1000);
                        ALTER TABLE classroom_sessions ADD COLUMN IF NOT EXISTS recording_sync_attempts INTEGER NOT NULL DEFAULT 0;
                        ALTER TABLE classroom_sessions ADD COLUMN IF NOT EXISTS recording_published_at TIMESTAMP;
                        ALTER TABLE classroom_sessions ADD COLUMN IF NOT EXISTS recording_expires_at TIMESTAMP;
                        UPDATE classroom_sessions
                        SET recording_sync_status = 'NOT_AVAILABLE'
                        WHERE recording_sync_status IS NULL;
                        UPDATE classroom_sessions
                        SET recording_sync_attempts = 0
                        WHERE recording_sync_attempts IS NULL;
                        ALTER TABLE classroom_sessions ALTER COLUMN recording_sync_status SET DEFAULT 'NOT_AVAILABLE';
                        ALTER TABLE classroom_sessions ALTER COLUMN recording_sync_status SET NOT NULL;
                        ALTER TABLE classroom_sessions ALTER COLUMN recording_sync_attempts SET DEFAULT 0;
                        ALTER TABLE classroom_sessions ALTER COLUMN recording_sync_attempts SET NOT NULL;
                    END IF;
                END $$;
                """);
    }
}
