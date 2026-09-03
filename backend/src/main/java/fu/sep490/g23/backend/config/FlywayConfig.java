package fu.sep490.g23.backend.config;

import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            // Automatically repair flyway_schema_history (delete failed migration records and fix checksums)
            // before running migration, ensuring smooth deployments without requiring manual DB access.
            flyway.repair();
            flyway.migrate();
        };
    }
}
