-- Manual rollback for OnlineCourseVersionSchemaMigration.
-- Run only after exporting online_course_versions and verifying that no enrollment
-- relies on retired snapshots. The operation intentionally does not delete course,
-- enrollment, lesson, or progress rows.

DROP INDEX IF EXISTS idx_lesson_progress_version_key;
DROP INDEX IF EXISTS idx_enrollment_course_version;
DROP INDEX IF EXISTS idx_lesson_key;
DROP INDEX IF EXISTS uk_online_course_single_published_version;
DROP INDEX IF EXISTS uk_online_course_single_open_version;

ALTER TABLE lesson_progress DROP COLUMN IF EXISTS course_version_id;
ALTER TABLE lesson_progress DROP COLUMN IF EXISTS lesson_key;
ALTER TABLE package_enrollments DROP COLUMN IF EXISTS course_version_id;
ALTER TABLE lessons DROP COLUMN IF EXISTS lesson_key;

DROP TABLE IF EXISTS online_course_versions;
