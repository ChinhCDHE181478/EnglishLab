-- Canonical ownership after the course/classroom refactor:
-- instructor_led_courses owns academic metadata, rooms owns physical address,
-- and class_sections owns only class execution data. class_sections.room_id stays unchanged.

WITH room_source AS (
    SELECT room_id, MIN(BTRIM(offline_address)) AS address
    FROM class_sections
    WHERE room_id IS NOT NULL AND NULLIF(BTRIM(offline_address), '') IS NOT NULL
    GROUP BY room_id
    HAVING COUNT(DISTINCT BTRIM(offline_address)) = 1
)
UPDATE rooms room
SET location_address = source.address
FROM room_source source
WHERE room.id = source.room_id
  AND NULLIF(BTRIM(room.location_address), '') IS NULL;

WITH entry_source AS (
    SELECT instructor_led_course_id, MIN(BTRIM(entry_level)) AS value
    FROM class_sections
    WHERE NULLIF(BTRIM(entry_level), '') IS NOT NULL
    GROUP BY instructor_led_course_id
    HAVING COUNT(DISTINCT BTRIM(entry_level)) = 1
)
UPDATE instructor_led_courses course
SET entry_level = source.value
FROM entry_source source
WHERE course.id = source.instructor_led_course_id
  AND NULLIF(BTRIM(course.entry_level), '') IS NULL;

WITH outcome_source AS (
    SELECT instructor_led_course_id, MIN(BTRIM(program_outcomes)) AS value
    FROM class_sections
    WHERE NULLIF(BTRIM(program_outcomes), '') IS NOT NULL
    GROUP BY instructor_led_course_id
    HAVING COUNT(DISTINCT BTRIM(program_outcomes)) = 1
), target_source AS (
    SELECT instructor_led_course_id, MIN(BTRIM(target_outcome)) AS value
    FROM class_sections
    WHERE NULLIF(BTRIM(target_outcome), '') IS NOT NULL
    GROUP BY instructor_led_course_id
    HAVING COUNT(DISTINCT BTRIM(target_outcome)) = 1
), syllabus_source AS (
    SELECT instructor_led_course_id, MIN(BTRIM(syllabus_summary)) AS value
    FROM class_sections
    WHERE NULLIF(BTRIM(syllabus_summary), '') IS NOT NULL
    GROUP BY instructor_led_course_id
    HAVING COUNT(DISTINCT BTRIM(syllabus_summary)) = 1
)
UPDATE instructor_led_courses course
SET learning_outcomes = COALESCE(program.value, target.value, syllabus.value)
FROM outcome_source program
FULL JOIN target_source target USING (instructor_led_course_id)
FULL JOIN syllabus_source syllabus USING (instructor_led_course_id)
WHERE course.id = COALESCE(program.instructor_led_course_id, target.instructor_led_course_id,
                           syllabus.instructor_led_course_id)
  AND NULLIF(BTRIM(course.learning_outcomes), '') IS NULL
  AND COALESCE(program.value, target.value, syllabus.value) IS NOT NULL;

WITH guide_source AS (
    SELECT instructor_led_course_id, MIN(BTRIM(teacher_guide)) AS value
    FROM class_sections
    WHERE NULLIF(BTRIM(teacher_guide), '') IS NOT NULL
    GROUP BY instructor_led_course_id
    HAVING COUNT(DISTINCT BTRIM(teacher_guide)) = 1
)
UPDATE instructor_led_courses course
SET teacher_guide = source.value
FROM guide_source source
WHERE course.id = source.instructor_led_course_id
  AND NULLIF(BTRIM(course.teacher_guide), '') IS NULL;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM class_sections section
        LEFT JOIN instructor_led_courses course ON course.id = section.instructor_led_course_id
        WHERE course.id IS NULL
    ) THEN
        RAISE EXCEPTION 'V30 found ClassSection rows without a valid InstructorLedCourse';
    END IF;

    IF EXISTS (
        SELECT 1 FROM class_sections section
        JOIN rooms room ON room.id = section.room_id
        WHERE NULLIF(BTRIM(room.location_address), '') IS NULL
          AND NULLIF(BTRIM(section.offline_address), '') IS NOT NULL
        GROUP BY section.room_id
        HAVING COUNT(DISTINCT BTRIM(section.offline_address)) > 1
    ) THEN
        RAISE EXCEPTION 'V30 found ambiguous ClassSection addresses for a Room with no canonical address';
    END IF;
END $$;

ALTER TABLE class_sections
    DROP COLUMN entry_level,
    DROP COLUMN target_outcome,
    DROP COLUMN offline_address,
    DROP COLUMN syllabus_summary,
    DROP COLUMN program_outcomes,
    DROP COLUMN teacher_guide,
    DROP COLUMN interaction_activities;

ALTER TABLE classroom_proposals
    DROP COLUMN offline_address,
    DROP COLUMN virtual_meeting_url;

ALTER TABLE online_courses
    DROP COLUMN study_mode;
