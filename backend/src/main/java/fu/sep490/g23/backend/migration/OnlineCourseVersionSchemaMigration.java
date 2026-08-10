package fu.sap490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OnlineCourseVersionSchemaMigration implements CommandLineRunner {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS online_course_versions (
                    id BIGSERIAL PRIMARY KEY,
                    online_course_id BIGINT NOT NULL REFERENCES online_courses(id),
                    version_number INTEGER NOT NULL,
                    status VARCHAR(30) NOT NULL,
                    content_snapshot_json TEXT NOT NULL DEFAULT '{}',
                    assessment_ids_json TEXT NOT NULL DEFAULT '[]',
                    total_required_lessons INTEGER NOT NULL DEFAULT 0,
                    total_required_assessments INTEGER NOT NULL DEFAULT 0,
                    change_note VARCHAR(700),
                    review_note VARCHAR(700),
                    created_by_id BIGINT REFERENCES users(id),
                    reviewed_by_id BIGINT REFERENCES users(id),
                    submitted_at TIMESTAMP,
                    published_at TIMESTAMP,
                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT uk_online_course_version_number UNIQUE (online_course_id, version_number)
                );

                ALTER TABLE package_enrollments
                    ADD COLUMN IF NOT EXISTS course_version_id BIGINT REFERENCES online_course_versions(id);
                ALTER TABLE online_course_versions
                    ADD COLUMN IF NOT EXISTS review_note VARCHAR(700);
                ALTER TABLE online_course_versions
                    ADD COLUMN IF NOT EXISTS assessment_ids_json TEXT;
                UPDATE online_course_versions
                    SET assessment_ids_json = '[]'
                    WHERE assessment_ids_json IS NULL OR trim(assessment_ids_json) = '';
                ALTER TABLE online_course_versions
                    ALTER COLUMN assessment_ids_json SET DEFAULT '[]';
                ALTER TABLE online_course_versions
                    ALTER COLUMN assessment_ids_json SET NOT NULL;
                ALTER TABLE lessons
                    ADD COLUMN IF NOT EXISTS lesson_key VARCHAR(120);
                ALTER TABLE lesson_progress
                    ADD COLUMN IF NOT EXISTS course_version_id BIGINT REFERENCES online_course_versions(id),
                    ADD COLUMN IF NOT EXISTS lesson_key VARCHAR(120);

                UPDATE lessons lesson
                SET lesson_key = 'COURSE-' || module.online_course_id || '-LESSON-' || lesson.id
                FROM course_modules module
                WHERE lesson.module_id = module.id
                  AND (lesson.lesson_key IS NULL OR trim(lesson.lesson_key) = '');

                INSERT INTO online_course_versions (
                    online_course_id,
                    version_number,
                    status,
                    content_snapshot_json,
                    assessment_ids_json,
                    total_required_lessons,
                    total_required_assessments,
                    created_by_id,
                    published_at,
                    created_at,
                    updated_at
                )
                SELECT course.id,
                       1,
                       CASE WHEN package.status = 'PUBLISHED' THEN 'PUBLISHED' ELSE 'DRAFT' END,
                       '{}',
                       '[]',
                       COALESCE(lesson_count.total_lessons, 0),
                       COALESCE(assessment_count.total_assessments, 0),
                       package.created_by_id,
                       CASE WHEN package.status = 'PUBLISHED' THEN CURRENT_TIMESTAMP ELSE NULL END,
                       CURRENT_TIMESTAMP,
                       CURRENT_TIMESTAMP
                FROM online_courses course
                JOIN packages package ON package.id = course.package_id
                LEFT JOIN (
                    SELECT module.online_course_id, COUNT(lesson.id) AS total_lessons
                    FROM course_modules module
                    LEFT JOIN lessons lesson ON lesson.module_id = module.id
                    GROUP BY module.online_course_id
                ) lesson_count ON lesson_count.online_course_id = course.id
                LEFT JOIN (
                    SELECT online_course_id, COUNT(*) AS total_assessments
                    FROM course_assessments
                    WHERE active = TRUE
                    GROUP BY online_course_id
                ) assessment_count ON assessment_count.online_course_id = course.id
                WHERE NOT EXISTS (
                    SELECT 1 FROM online_course_versions version
                    WHERE version.online_course_id = course.id
                );

                UPDATE package_enrollments enrollment
                SET course_version_id = version.id
                FROM online_courses course
                JOIN online_course_versions version
                  ON version.online_course_id = course.id
                 AND version.version_number = 1
                WHERE enrollment.package_id = course.package_id
                  AND enrollment.course_version_id IS NULL;

                UPDATE lesson_progress progress
                SET course_version_id = enrollment.course_version_id,
                    lesson_key = lesson.lesson_key
                FROM package_enrollments enrollment, lessons lesson
                WHERE progress.enrollment_id = enrollment.id
                  AND progress.lesson_id = lesson.id
                  AND (progress.course_version_id IS NULL OR progress.lesson_key IS NULL);

                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1
                        FROM package_enrollments enrollment
                        JOIN online_courses course ON course.package_id = enrollment.package_id
                        WHERE enrollment.course_version_id IS NULL
                    ) THEN
                        RAISE EXCEPTION 'Course version backfill incomplete: online enrollment without version';
                    END IF;
                    IF EXISTS (
                        SELECT 1
                        FROM lessons lesson
                        WHERE lesson.lesson_key IS NULL OR trim(lesson.lesson_key) = ''
                    ) THEN
                        RAISE EXCEPTION 'Course version backfill incomplete: lesson without lesson_key';
                    END IF;
                END $$;

                ALTER TABLE lessons
                    ALTER COLUMN lesson_key SET NOT NULL;

                CREATE INDEX IF NOT EXISTS idx_lesson_key
                    ON lessons(lesson_key);
                CREATE INDEX IF NOT EXISTS idx_enrollment_course_version
                    ON package_enrollments(course_version_id);
                CREATE INDEX IF NOT EXISTS idx_lesson_progress_version_key
                    ON lesson_progress(course_version_id, lesson_key);
                CREATE UNIQUE INDEX IF NOT EXISTS uk_online_course_single_published_version
                    ON online_course_versions(online_course_id)
                    WHERE status = 'PUBLISHED';
                CREATE UNIQUE INDEX IF NOT EXISTS uk_online_course_single_open_version
                    ON online_course_versions(online_course_id)
                    WHERE status IN ('DRAFT', 'PENDING_REVIEW');
                """);
    }
}
