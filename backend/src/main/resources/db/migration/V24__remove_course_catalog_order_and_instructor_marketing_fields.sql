ALTER TABLE online_courses
    DROP COLUMN IF EXISTS display_order;

ALTER TABLE instructor_led_courses
    DROP COLUMN IF EXISTS display_order,
    DROP COLUMN IF EXISTS featured,
    DROP COLUMN IF EXISTS thumbnail_url;
