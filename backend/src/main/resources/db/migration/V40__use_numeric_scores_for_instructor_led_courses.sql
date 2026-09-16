-- Normalize legacy display labels before instructor-led recommendations switch to numeric scores.
UPDATE instructor_led_courses
SET entry_level = (regexp_match(entry_level, '(?i)IELTS\s*([0-9](?:\.[0-9]+)?)'))[1]
WHERE UPPER(exam_type) = 'IELTS'
  AND entry_level ~* 'IELTS\s*[0-9]';

UPDATE instructor_led_courses
SET entry_level = (regexp_match(entry_level, '(?i)TOEIC\s*([0-9]{2,3})'))[1]
WHERE UPPER(exam_type) = 'TOEIC'
  AND entry_level ~* 'TOEIC\s*[0-9]';

-- Recover missing numeric targets when legacy learning outcomes explicitly state "mục tiêu".
WITH parsed_ielts_targets AS (
    SELECT id,
           regexp_match(
               learning_outcomes,
               '(?i)mục tiêu\s*(?:band\s*)?([0-9](?:\.[0-9]+)?)(?:\s*[-–]\s*([0-9](?:\.[0-9]+)?))?'
           ) AS scores
    FROM instructor_led_courses
    WHERE UPPER(exam_type) = 'IELTS'
      AND target_band IS NULL
      AND learning_outcomes ~* 'mục tiêu\s*(?:band\s*)?[0-9]'
)
UPDATE instructor_led_courses AS course
SET target_band = COALESCE(parsed.scores[2], parsed.scores[1])::numeric
FROM parsed_ielts_targets AS parsed
WHERE course.id = parsed.id
  AND COALESCE(parsed.scores[2], parsed.scores[1])::numeric BETWEEN 0 AND 9;

WITH parsed_toeic_targets AS (
    SELECT id,
           (regexp_match(learning_outcomes, '(?i)mục tiêu\s*(?:điểm\s*)?([0-9]{2,3})'))[1] AS score
    FROM instructor_led_courses
    WHERE UPPER(exam_type) = 'TOEIC'
      AND target_score IS NULL
      AND learning_outcomes ~* 'mục tiêu\s*(?:điểm\s*)?[0-9]'
)
UPDATE instructor_led_courses AS course
SET target_score = parsed.score::integer
FROM parsed_toeic_targets AS parsed
WHERE course.id = parsed.id
  AND parsed.score::integer BETWEEN 10 AND 990;

ALTER TABLE instructor_led_courses
    DROP COLUMN IF EXISTS level,
    DROP COLUMN IF EXISTS entry_placement_level;
