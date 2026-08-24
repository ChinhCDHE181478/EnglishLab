package fu.sep490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPreferenceSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureNotificationPreferenceTable() {
        jdbcTemplate.execute("""
                ALTER TABLE app_notifications
                    ADD COLUMN IF NOT EXISTS action_path VARCHAR(500);
                ALTER TABLE app_notifications
                    ADD COLUMN IF NOT EXISTS deduplication_key VARCHAR(220);
                CREATE UNIQUE INDEX IF NOT EXISTS uk_app_notification_user_deduplication
                    ON app_notifications(user_id, deduplication_key)
                    WHERE deduplication_key IS NOT NULL;
                """);
    }
}
