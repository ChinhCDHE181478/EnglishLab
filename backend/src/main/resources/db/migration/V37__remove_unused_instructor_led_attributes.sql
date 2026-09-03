ALTER TABLE instructor_led_courses
    DROP COLUMN IF EXISTS slug,
    DROP COLUMN IF EXISTS program_track;

DROP INDEX IF EXISTS uk_course_units_ilc_code;

ALTER TABLE course_units
    DROP COLUMN IF EXISTS code;

ALTER TABLE course_unit_content_refs
    DROP COLUMN IF EXISTS note;

ALTER TABLE class_sections
    DROP COLUMN IF EXISTS location_note;
