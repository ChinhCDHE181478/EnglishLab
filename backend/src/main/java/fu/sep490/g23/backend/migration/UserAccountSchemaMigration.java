package fu.sap490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserAccountSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureAvatarColumn() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.users') IS NOT NULL THEN
                        ALTER TABLE users ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(500);
                    END IF;
                END $$;
                """);
    }
}
