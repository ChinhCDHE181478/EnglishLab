package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 240)
@RequiredArgsConstructor
public class ClassroomPracticeAttemptMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.classroom_practice_attempts') IS NOT NULL THEN
                        INSERT INTO classroom_practice_attempt_history (
                            classroom_offering_id,
                            student_id,
                            exercise_id,
                            attempt_number,
                            response_text,
                            completed_at,
                            created_at
                        )
                        SELECT
                            legacy.classroom_offering_id,
                            legacy.student_id,
                            legacy.exercise_id,
                            1,
                            legacy.response_text,
                            legacy.completed_at,
                            COALESCE(legacy.created_at, legacy.completed_at)
                        FROM classroom_practice_attempts legacy
                        WHERE NOT EXISTS (
                            SELECT 1
                            FROM classroom_practice_attempt_history history
                            WHERE history.classroom_offering_id = legacy.classroom_offering_id
                              AND history.student_id = legacy.student_id
                              AND history.exercise_id = legacy.exercise_id
                        );

                        DROP TABLE classroom_practice_attempts;
                    END IF;
                END $$;
                """);
    }
}
