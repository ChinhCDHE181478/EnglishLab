package fu.sep490.g23.backend.migration;

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

                ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS exam_category VARCHAR(30);
                ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS target_band NUMERIC(3,1);
                ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS target_score INTEGER;
                ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS discount_percent INTEGER;
                ALTER TABLE learning_paths ADD COLUMN IF NOT EXISTS minimum_courses_for_discount INTEGER;
                UPDATE learning_paths SET discount_percent = 0 WHERE discount_percent IS NULL;
                UPDATE learning_paths SET minimum_courses_for_discount = 2 WHERE minimum_courses_for_discount IS NULL;
                ALTER TABLE learning_paths ALTER COLUMN discount_percent SET DEFAULT 0;
                ALTER TABLE learning_paths ALTER COLUMN discount_percent SET NOT NULL;
                ALTER TABLE learning_paths ALTER COLUMN minimum_courses_for_discount SET DEFAULT 2;
                ALTER TABLE learning_paths ALTER COLUMN minimum_courses_for_discount SET NOT NULL;
                """);

        jdbcTemplate.execute("""
                INSERT INTO learning_paths (code, name, discount_percent, minimum_courses_for_discount)
                SELECT DISTINCT trim(learning_path_code),
                       COALESCE(NULLIF(max(trim(learning_path_name)), ''), trim(learning_path_code)),
                       0,
                       2
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
