package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 230)
@RequiredArgsConstructor
public class UserNotificationPreferenceMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        jdbcTemplate.execute("""
                ALTER TABLE users
                    ADD COLUMN IF NOT EXISTS notification_email_enabled BOOLEAN,
                    ADD COLUMN IF NOT EXISTS notification_in_app_enabled BOOLEAN,
                    ADD COLUMN IF NOT EXISTS notification_class_reminder_enabled BOOLEAN,
                    ADD COLUMN IF NOT EXISTS notification_study_alert_enabled BOOLEAN;

                DO $$
                BEGIN
                    IF to_regclass('public.notification_preferences') IS NOT NULL THEN
                        UPDATE users user_account
                        SET notification_email_enabled = preference.email_enabled,
                            notification_in_app_enabled = preference.in_app_enabled,
                            notification_class_reminder_enabled = preference.class_reminder_enabled,
                            notification_study_alert_enabled = preference.study_alert_enabled
                        FROM notification_preferences preference
                        WHERE preference.user_id = user_account.id;
                    END IF;
                END $$;

                UPDATE users SET
                    notification_email_enabled = COALESCE(notification_email_enabled, TRUE),
                    notification_in_app_enabled = COALESCE(notification_in_app_enabled, TRUE),
                    notification_class_reminder_enabled = COALESCE(notification_class_reminder_enabled, TRUE),
                    notification_study_alert_enabled = COALESCE(notification_study_alert_enabled, TRUE);

                ALTER TABLE users
                    ALTER COLUMN notification_email_enabled SET DEFAULT TRUE,
                    ALTER COLUMN notification_email_enabled SET NOT NULL,
                    ALTER COLUMN notification_in_app_enabled SET DEFAULT TRUE,
                    ALTER COLUMN notification_in_app_enabled SET NOT NULL,
                    ALTER COLUMN notification_class_reminder_enabled SET DEFAULT TRUE,
                    ALTER COLUMN notification_class_reminder_enabled SET NOT NULL,
                    ALTER COLUMN notification_study_alert_enabled SET DEFAULT TRUE,
                    ALTER COLUMN notification_study_alert_enabled SET NOT NULL;

                DROP TABLE IF EXISTS notification_preferences;
                """);
    }
}
