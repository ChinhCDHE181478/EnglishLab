package fu.sep490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CourseContentOrderSchemaMigration implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                WITH normalized_modules AS (
                    SELECT id,
                           ROW_NUMBER() OVER (
                               PARTITION BY online_course_id
                               ORDER BY display_order ASC, id ASC
                           ) AS normalized_order
                    FROM course_modules
                )
                UPDATE course_modules target
                SET display_order = normalized.normalized_order
                FROM normalized_modules normalized
                WHERE target.id = normalized.id
                  AND target.display_order <> normalized.normalized_order;

                WITH normalized_lessons AS (
                    SELECT id,
                           ROW_NUMBER() OVER (
                               PARTITION BY module_id
                               ORDER BY display_order ASC, id ASC
                           ) AS normalized_order
                    FROM lessons
                )
                UPDATE lessons target
                SET display_order = normalized.normalized_order
                FROM normalized_lessons normalized
                WHERE target.id = normalized.id
                  AND target.display_order <> normalized.normalized_order;

                DO $$
                BEGIN
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint WHERE conname = 'uk_course_module_order'
                    ) THEN
                        ALTER TABLE course_modules
                            ADD CONSTRAINT uk_course_module_order UNIQUE (online_course_id, display_order);
                    END IF;
                    IF NOT EXISTS (
                        SELECT 1 FROM pg_constraint WHERE conname = 'uk_lesson_order'
                    ) THEN
                        ALTER TABLE lessons
                            ADD CONSTRAINT uk_lesson_order UNIQUE (module_id, display_order);
                    END IF;
                END $$;
                """);
    }
}
