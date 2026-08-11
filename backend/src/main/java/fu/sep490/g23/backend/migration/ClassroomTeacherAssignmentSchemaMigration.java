package fu.sep490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClassroomTeacherAssignmentSchemaMigration {

    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void ensureSessionScopedAssignmentColumn() {
        jdbcTemplate.execute("""
                DO $$
                BEGIN
                    IF to_regclass('public.classroom_teacher_assignments') IS NOT NULL
                       AND to_regclass('public.classroom_sessions') IS NOT NULL THEN
                        ALTER TABLE classroom_teacher_assignments
                            ADD COLUMN IF NOT EXISTS classroom_session_id BIGINT;

                        IF NOT EXISTS (
                            SELECT 1 FROM pg_constraint
                            WHERE conname = 'fk_teacher_assignment_session'
                        ) THEN
                            ALTER TABLE classroom_teacher_assignments
                                ADD CONSTRAINT fk_teacher_assignment_session
                                FOREIGN KEY (classroom_session_id)
                                REFERENCES classroom_sessions(id)
                                ON DELETE CASCADE;
                        END IF;

                        CREATE UNIQUE INDEX IF NOT EXISTS uk_teacher_assignment_session
                            ON classroom_teacher_assignments(classroom_session_id)
                            WHERE classroom_session_id IS NOT NULL;
                    END IF;
                END $$;
                """);
    }
}
