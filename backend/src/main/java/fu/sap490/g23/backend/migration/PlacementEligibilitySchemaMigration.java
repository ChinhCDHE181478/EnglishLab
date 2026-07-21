package fu.sap490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PlacementEligibilitySchemaMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                ALTER TABLE placement_test_attempts
                    ADD COLUMN IF NOT EXISTS evaluation_status VARCHAR(40) NOT NULL DEFAULT 'SUBMITTED',
                    ADD COLUMN IF NOT EXISTS recommended_level VARCHAR(30),
                    ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP,
                    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP,
                    ADD COLUMN IF NOT EXISTS fraud_suspected BOOLEAN NOT NULL DEFAULT FALSE,
                    ADD COLUMN IF NOT EXISTS reviewer_id BIGINT,
                    ADD COLUMN IF NOT EXISTS reviewed_at TIMESTAMP,
                    ADD COLUMN IF NOT EXISTS review_note VARCHAR(700);

                UPDATE placement_test_attempts
                SET expires_at = submitted_at + INTERVAL '180 days'
                WHERE expires_at IS NULL;

                UPDATE placement_test_attempts
                SET evaluation_status = CASE
                        WHEN ai_feedback_json LIKE '%\"examType\":\"TOEIC\"%'
                             AND status = 'COMPLETED' THEN 'ELIGIBLE'
                        WHEN status IN ('OBJECTIVE_EVALUATED', 'COMPLETED') THEN 'MANUAL_REVIEW_REQUIRED'
                        ELSE 'SUBMITTED'
                    END
                WHERE evaluation_status IS NULL OR evaluation_status = 'SUBMITTED';

                UPDATE placement_test_attempts
                SET recommended_level = CASE
                        WHEN overall_score < 450 THEN 'BEGINNER'
                        WHEN overall_score < 700 THEN 'INTERMEDIATE'
                        ELSE 'ADVANCED'
                    END
                WHERE evaluation_status = 'ELIGIBLE'
                  AND recommended_level IS NULL
                  AND overall_score IS NOT NULL;

                CREATE INDEX IF NOT EXISTS idx_placement_attempt_evaluation_status
                    ON placement_test_attempts(evaluation_status, submitted_at);
                """);
    }
}
