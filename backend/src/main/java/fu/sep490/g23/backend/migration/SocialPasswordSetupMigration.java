package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialPasswordSetupMigration implements CommandLineRunner {

    private static final String MIGRATION_KEY = "social_password_setup_v1";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS application_schema_migrations (
                    migration_key VARCHAR(100) PRIMARY KEY,
                    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);

        int inserted = jdbcTemplate.update("""
                INSERT INTO application_schema_migrations (migration_key)
                VALUES (?)
                ON CONFLICT (migration_key) DO NOTHING
                """, MIGRATION_KEY);
        if (inserted == 0) {
            return;
        }

        jdbcTemplate.execute("""
                ALTER TABLE users
                ADD COLUMN IF NOT EXISTS password_set BOOLEAN NOT NULL DEFAULT TRUE
                """);
        jdbcTemplate.execute("""
                UPDATE users
                SET password_set = CASE
                    WHEN google_id IS NOT NULL OR facebook_id IS NOT NULL THEN FALSE
                    ELSE TRUE
                END
                """);
    }
}
