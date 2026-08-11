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
                CREATE TABLE IF NOT EXISTS notification_preferences (
                    id BIGSERIAL PRIMARY KEY,
                    user_id BIGINT NOT NULL UNIQUE,
                    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_notification_preferences_user
                        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                );

                CREATE UNIQUE INDEX IF NOT EXISTS uk_notification_preferences_user
                    ON notification_preferences (user_id);

                ALTER TABLE notification_preferences
                    ADD COLUMN IF NOT EXISTS class_reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE;
                ALTER TABLE notification_preferences
                    ADD COLUMN IF NOT EXISTS study_alert_enabled BOOLEAN NOT NULL DEFAULT TRUE;

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
