package fu.sap490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MockTestAttemptSchemaMigration {
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureMockTestAttemptTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS mock_test_attempts (
                    id BIGSERIAL PRIMARY KEY,
                    assessment_bank_item_id BIGINT NOT NULL REFERENCES assessment_bank_items(id),
                    student_id BIGINT NOT NULL REFERENCES users(id),
                    skill VARCHAR(30) NOT NULL,
                    objective_answers_json TEXT,
                    submitted_text TEXT,
                    submitted_audio_url VARCHAR(700),
                    correct_count INTEGER,
                    total_questions INTEGER,
                    score NUMERIC(6,2),
                    percent NUMERIC(6,2),
                    status VARCHAR(30) NOT NULL,
                    submitted_at TIMESTAMP NOT NULL
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_mock_test_attempts_student
                ON mock_test_attempts(student_id, submitted_at DESC)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_mock_test_attempts_item_student
                ON mock_test_attempts(assessment_bank_item_id, student_id, submitted_at DESC)
                """);
    }
}
