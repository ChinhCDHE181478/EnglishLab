ALTER TABLE course_assessments
    ADD COLUMN online_lesson_id BIGINT;

ALTER TABLE course_assessments
    ADD CONSTRAINT fk_course_assessments_online_lesson
        FOREIGN KEY (online_lesson_id) REFERENCES online_lessons(id);

CREATE UNIQUE INDEX uk_course_assessments_active_online_lesson
    ON course_assessments (online_lesson_id)
    WHERE online_lesson_id IS NOT NULL AND active = TRUE;

CREATE INDEX idx_course_assessments_online_lesson
    ON course_assessments (online_lesson_id);

-- Legacy lesson rows only contained rich text. Keep that text as an article instead
-- of presenting it as a functional quiz/assignment without a real assessment.
UPDATE online_lessons
SET content_type = 'ARTICLE'
WHERE UPPER(COALESCE(content_type, '')) IN ('QUIZ', 'ASSIGNMENT')
  AND NOT EXISTS (
      SELECT 1
      FROM course_assessments assessment
      WHERE assessment.online_lesson_id = online_lessons.id
        AND assessment.active = TRUE
  );
