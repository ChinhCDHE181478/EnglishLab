ALTER TABLE online_course_enrollments
    ADD COLUMN IF NOT EXISTS completed_at timestamp(6) without time zone;

WITH completion_activity AS (
    SELECT enrollment.id AS enrollment_id,
           GREATEST(
               MAX(progress.completed_at),
               MAX(submission.submitted_at)
           ) AS completed_at
    FROM online_course_enrollments enrollment
    LEFT JOIN lesson_progress progress
        ON progress.online_course_enrollment_id = enrollment.id
       AND progress.status = 'COMPLETED'
    LEFT JOIN course_assessments assessment
        ON assessment.online_course_version_id = enrollment.course_version_id
       AND assessment.active = true
    LEFT JOIN assessment_submissions submission
        ON submission.assessment_id = assessment.id
       AND submission.student_id = enrollment.student_id
       AND submission.status IN ('PASSED', 'AI_EVALUATED')
    WHERE enrollment.status = 'COMPLETED'
    GROUP BY enrollment.id
)
UPDATE online_course_enrollments enrollment
SET completed_at = COALESCE(
    activity.completed_at,
    enrollment.updated_at,
    enrollment.registered_at
)
FROM completion_activity activity
WHERE enrollment.id = activity.enrollment_id
  AND enrollment.completed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_online_course_enrollments_completed_at
    ON online_course_enrollments (completed_at);
