package fu.sap490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlacementTestToeicSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureToeicPlacementColumns() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.placement_test_definitions') IS NOT NULL THEN
                        ALTER TABLE placement_test_definitions
                            ADD COLUMN IF NOT EXISTS exam_type VARCHAR(20) NOT NULL DEFAULT 'IELTS';

                        ALTER TABLE placement_test_definitions
                            ADD COLUMN IF NOT EXISTS toeic_config_json TEXT;
                    END IF;
                END $$;
                """);
    }
}
