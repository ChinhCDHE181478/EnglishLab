package fu.sap490.g23.backend.migration;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LearningPathSchemaMigration {
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateLearningPaths() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS learning_paths (
                    id BIGSERIAL PRIMARY KEY,
                    code VARCHAR(80) NOT NULL UNIQUE,
                    name VARCHAR(180) NOT NULL
                );

                CREATE TABLE IF NOT EXISTS learning_path_courses (
                    id BIGSERIAL PRIMARY KEY,
                    learning_path_id BIGINT NOT NULL REFERENCES learning_paths(id) ON DELETE CASCADE,
                    online_course_id BIGINT NOT NULL REFERENCES online_courses(id) ON DELETE CASCADE,
                    display_order INTEGER NOT NULL,
                    CONSTRAINT uk_learning_path_course UNIQUE (learning_path_id, online_course_id)
                );

                CREATE INDEX IF NOT EXISTS idx_learning_path_courses_path_order
                    ON learning_path_courses(learning_path_id, display_order, id);
                """);

        jdbcTemplate.execute("""
                INSERT INTO learning_paths (code, name)
                SELECT DISTINCT trim(learning_path_code),
                       COALESCE(NULLIF(max(trim(learning_path_name)), ''), trim(learning_path_code))
                FROM online_courses
                WHERE learning_path_code IS NOT NULL
                  AND trim(learning_path_code) <> ''
                GROUP BY trim(learning_path_code)
                ON CONFLICT (code) DO NOTHING;

                INSERT INTO learning_path_courses (learning_path_id, online_course_id, display_order)
                SELECT path.id, course.id, COALESCE(course.learning_path_order, 0)
                FROM online_courses course
                JOIN learning_paths path ON path.code = trim(course.learning_path_code)
                WHERE course.learning_path_code IS NOT NULL
                  AND trim(course.learning_path_code) <> ''
                ON CONFLICT (learning_path_id, online_course_id) DO NOTHING;
                """);
    }
}
