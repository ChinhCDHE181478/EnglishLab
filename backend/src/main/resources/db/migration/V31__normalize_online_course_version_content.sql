-- Replace duplicated online-course metadata with canonical relational ownership.

ALTER TABLE course_assessments
    ADD COLUMN online_course_version_id BIGINT;

-- Module/lesson ownership is authoritative when present.
UPDATE course_assessments assessment
SET online_course_version_id = module.online_course_version_id
FROM online_course_modules module
WHERE assessment.module_id = module.id;

UPDATE course_assessments assessment
SET online_course_version_id = module.online_course_version_id
FROM online_lessons lesson
JOIN online_course_modules module ON module.id = lesson.module_id
WHERE assessment.online_course_version_id IS NULL
  AND assessment.online_lesson_id = lesson.id;

-- Historical course-level assessments are mapped from the immutable version ID list.
WITH version_assessment AS (
    SELECT version.id AS version_id, value::BIGINT AS assessment_id
    FROM online_course_versions version
    CROSS JOIN LATERAL jsonb_array_elements_text(
        COALESCE(NULLIF(version.assessment_ids_json, ''), '[]')::jsonb
    ) value
), unambiguous AS (
    SELECT assessment_id, MIN(version_id) AS version_id
    FROM version_assessment
    GROUP BY assessment_id
    HAVING COUNT(DISTINCT version_id) = 1
)
UPDATE course_assessments assessment
SET online_course_version_id = mapping.version_id
FROM unambiguous mapping
WHERE assessment.id = mapping.assessment_id
  AND assessment.online_course_version_id IS NULL;

-- Active rows not present in an old JSON list belong to the current working version.
WITH ranked_version AS (
    SELECT id, online_course_id,
           ROW_NUMBER() OVER (
               PARTITION BY online_course_id
               ORDER BY CASE status
                   WHEN 'DRAFT' THEN 1
                   WHEN 'PENDING_REVIEW' THEN 2
                   WHEN 'PUBLISHED' THEN 3
                   ELSE 4
               END, version_number DESC, id DESC
           ) AS rank
    FROM online_course_versions
)
UPDATE course_assessments assessment
SET online_course_version_id = version.id
FROM ranked_version version
WHERE assessment.online_course_version_id IS NULL
  AND assessment.active = TRUE
  AND assessment.online_course_id = version.online_course_id
  AND version.rank = 1;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM course_assessments assessment
        JOIN online_course_versions version ON version.id = assessment.online_course_version_id
        WHERE assessment.online_course_id <> version.online_course_id
    ) THEN
        RAISE EXCEPTION 'V31 found CourseAssessment ownership crossing OnlineCourse boundaries';
    END IF;

    IF EXISTS (SELECT 1 FROM course_assessments WHERE online_course_version_id IS NULL) THEN
        RAISE EXCEPTION 'V31 cannot map every CourseAssessment to an OnlineCourseVersion';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM learner_lesson_notes note
        JOIN online_lessons lesson ON lesson.id = note.lesson_id
        JOIN online_course_modules module ON module.id = lesson.module_id
        JOIN online_course_versions version ON version.id = module.online_course_version_id
        WHERE note.course_id <> version.online_course_id
    ) THEN
        RAISE EXCEPTION 'V31 found LearnerLessonNote course_id inconsistent with its lesson';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM lesson_progress progress
        JOIN online_course_enrollments enrollment ON enrollment.id = progress.online_course_enrollment_id
        JOIN online_lessons lesson ON lesson.id = progress.online_lesson_id
        JOIN online_course_modules module ON module.id = lesson.module_id
        WHERE progress.student_id <> enrollment.student_id
           OR progress.course_version_id <> enrollment.course_version_id
           OR module.online_course_version_id <> enrollment.course_version_id
    ) THEN
        RAISE EXCEPTION 'V31 found LessonProgress ownership inconsistent with enrollment/version';
    END IF;
END $$;

ALTER TABLE course_assessments
    ALTER COLUMN online_course_version_id SET NOT NULL,
    ADD CONSTRAINT fk_course_assessments_online_course_version
        FOREIGN KEY (online_course_version_id) REFERENCES online_course_versions(id);

CREATE INDEX idx_course_assessments_version_order
    ON course_assessments (online_course_version_id, active, display_order, id);

UPDATE online_lessons
SET video_url = bunny_cdn_url
WHERE NULLIF(BTRIM(video_url), '') IS NULL
  AND bunny_cdn_url ~* '^https?://';

ALTER TABLE online_course_versions
    RENAME COLUMN reviewed_by_id TO published_by_id;

ALTER TABLE online_courses
    DROP COLUMN deleted,
    DROP COLUMN review_note,
    DROP COLUMN submitted_for_review_at,
    DROP COLUMN reviewed_at,
    DROP COLUMN reviewed_by_id;

ALTER TABLE online_course_versions
    DROP COLUMN content_snapshot_json,
    DROP COLUMN assessment_ids_json,
    DROP COLUMN review_note,
    DROP COLUMN submitted_at;

ALTER TABLE online_lessons
    DROP COLUMN bunny_video_id,
    DROP COLUMN bunny_library_id,
    DROP COLUMN bunny_cdn_url;

ALTER TABLE lesson_progress
    DROP COLUMN student_id,
    DROP COLUMN course_version_id,
    DROP COLUMN stable_lesson_key,
    DROP COLUMN progress_percent;

ALTER TABLE content_bank_items
    DROP COLUMN display_order;

ALTER TABLE learner_lesson_notes
    DROP COLUMN course_id;

ALTER TABLE course_discussion_posts
    DROP COLUMN reported_count,
    DROP COLUMN helpful_count;

ALTER TABLE course_assessments
    DROP COLUMN online_course_id;

ALTER TABLE course_discussion_reports
    DROP CONSTRAINT IF EXISTS fk33yujpu4i47rq535mo8e35qg0;
