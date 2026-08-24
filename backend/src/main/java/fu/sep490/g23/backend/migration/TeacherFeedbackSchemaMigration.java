package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(55)
@RequiredArgsConstructor
public class TeacherFeedbackSchemaMigration implements ApplicationRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        if (consolidated()) {
            return;
        }
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS teacher_course_feedback (
                    id BIGSERIAL PRIMARY KEY,
                    enrollment_id BIGINT NOT NULL REFERENCES classroom_enrollments(id),
                    classroom_offering_id BIGINT NOT NULL REFERENCES classroom_offerings(id),
                    teacher_id BIGINT NOT NULL REFERENCES users(id),
                    clarity_score INTEGER NOT NULL CHECK (clarity_score BETWEEN 1 AND 5),
                    engagement_score INTEGER NOT NULL CHECK (engagement_score BETWEEN 1 AND 5),
                    learner_support_score INTEGER NOT NULL CHECK (learner_support_score BETWEEN 1 AND 5),
                    feedback_timeliness_score INTEGER NOT NULL CHECK (feedback_timeliness_score BETWEEN 1 AND 5),
                    professionalism_score INTEGER NOT NULL CHECK (professionalism_score BETWEEN 1 AND 5),
                    pace VARCHAR(20) NOT NULL CHECK (pace IN ('TOO_SLOW', 'JUST_RIGHT', 'TOO_FAST')),
                    would_recommend BOOLEAN NOT NULL,
                    strengths VARCHAR(1500) NOT NULL,
                    improvement_suggestions VARCHAR(1500) NOT NULL,
                    additional_comment VARCHAR(1500),
                    submitted_at TIMESTAMP NOT NULL,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_teacher_feedback_enrollment_teacher UNIQUE (enrollment_id, teacher_id)
                )
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_teacher_course_feedback_teacher
                ON teacher_course_feedback(teacher_id, submitted_at DESC)
                """);
        jdbcTemplate.execute("""
                CREATE INDEX IF NOT EXISTS idx_teacher_course_feedback_classroom
                ON teacher_course_feedback(classroom_offering_id, teacher_id)
                """);
        jdbcTemplate.execute("""
                DO $$
                DECLARE constraint_name TEXT;
                BEGIN
                    SELECT conname INTO constraint_name
                    FROM pg_constraint
                    WHERE conrelid = 'teacher_course_feedback'::regclass
                      AND contype = 'c'
                      AND pg_get_constraintdef(oid) ILIKE '%pace%'
                      AND pg_get_constraintdef(oid) NOT ILIKE '%JUST_RIGHT%';
                    IF constraint_name IS NOT NULL THEN
                        EXECUTE format('ALTER TABLE teacher_course_feedback DROP CONSTRAINT %I', constraint_name);
                    END IF;
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint
                        WHERE conrelid = 'teacher_course_feedback'::regclass
                          AND conname = 'ck_teacher_course_feedback_pace'
                    ) THEN
                        ALTER TABLE teacher_course_feedback
                            ADD CONSTRAINT ck_teacher_course_feedback_pace
                            CHECK (pace IN ('TOO_SLOW', 'JUST_RIGHT', 'TOO_FAST'));
                    END IF;
                END $$;
                """);
    }

    private boolean consolidated() {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT to_regclass('public.user_auxiliary_records') IS NOT NULL",
                Boolean.class
        ));
    }
}
