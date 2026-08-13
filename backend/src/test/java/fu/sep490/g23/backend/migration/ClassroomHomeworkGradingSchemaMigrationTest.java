package fu.sep490.g23.backend.migration;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class ClassroomHomeworkGradingSchemaMigrationTest {

    @Test
    void ensureHomeworkGradingColumns_AllowsEveryJavaGradingMode() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        ClassroomHomeworkGradingSchemaMigration migration =
                new ClassroomHomeworkGradingSchemaMigration(jdbcTemplate);

        migration.ensureHomeworkGradingColumns();

        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        verify(jdbcTemplate).execute(sqlCaptor.capture());
        assertThat(sqlCaptor.getValue())
                .contains("DROP CONSTRAINT classroom_homework_grading_mode_check")
                .contains("CHECK (grading_mode IN ('TEACHER', 'AI', 'AUTO'))");
    }
}
