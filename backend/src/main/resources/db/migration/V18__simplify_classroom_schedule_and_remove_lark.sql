-- Move the shared Google Meet room to the class and simplify per-session schedules.

ALTER TABLE class_sections
    ADD COLUMN google_meet_owner_id bigint,
    ADD COLUMN google_meet_space_name varchar(255),
    ADD COLUMN google_meet_url varchar(700),
    ADD COLUMN google_meet_status varchar(30) NOT NULL DEFAULT 'NOT_CREATED',
    ADD COLUMN google_meet_sync_error varchar(1000);

UPDATE class_sections section
SET google_meet_owner_id = COALESCE(section.virtual_meeting_owner_id, section.primary_teacher_id),
    google_meet_url = CASE
        WHEN section.default_lark_meeting_url ~* '^https://meet\.google\.com/'
            THEN section.default_lark_meeting_url
        ELSE NULL
    END,
    google_meet_status = CASE
        WHEN section.default_lark_meeting_url ~* '^https://meet\.google\.com/' THEN 'READY'
        ELSE 'NOT_CREATED'
    END;

UPDATE class_sections section
SET google_meet_space_name = source.space_name,
    google_meet_url = COALESCE(section.google_meet_url, source.meeting_url),
    google_meet_status = 'READY',
    google_meet_sync_error = source.sync_error
FROM LATERAL (
    SELECT schedule.lark_meeting_id AS space_name,
           schedule.lark_meeting_url AS meeting_url,
           schedule.lark_sync_error AS sync_error
    FROM class_schedules schedule
    WHERE schedule.class_section_id = section.id
      AND schedule.lark_meeting_id LIKE 'spaces/%'
      AND schedule.lark_meeting_url ~* '^https://meet\.google\.com/'
    ORDER BY schedule.lark_synced_at DESC NULLS LAST, schedule.id DESC
    LIMIT 1
) source;

ALTER TABLE class_sections
    ADD CONSTRAINT fk_class_sections_google_meet_owner
        FOREIGN KEY (google_meet_owner_id) REFERENCES users(id) ON DELETE SET NULL,
    ADD CONSTRAINT ck_class_sections_google_meet_status
        CHECK (google_meet_status IN ('NOT_CREATED', 'CREATING', 'READY', 'FAILED'));

CREATE INDEX idx_class_sections_google_meet_owner
    ON class_sections(google_meet_owner_id);

UPDATE class_schedules schedule
SET session_content = COALESCE(
        NULLIF(BTRIM(schedule.session_content), ''),
        CASE schedule.schedule_type
            WHEN 'ORIENTATION' THEN 'Buổi định hướng'
            WHEN 'EXAM' THEN 'Buổi kiểm tra'
            WHEN 'MAKEUP' THEN 'Buổi học bù'
            ELSE 'Buổi học đặc biệt'
        END
    )
WHERE schedule.course_lesson_id IS NULL;

UPDATE class_schedules
SET status = 'SCHEDULED'
WHERE status IN ('MAKEUP', 'RESCHEDULED');

ALTER TABLE class_schedules
    DROP CONSTRAINT IF EXISTS ck_class_schedules_type,
    DROP CONSTRAINT IF EXISTS ck_class_schedules_lesson_required;

ALTER TABLE class_schedules
    RENAME COLUMN delivery_mode TO delivery_mode_override;

UPDATE class_schedules schedule
SET delivery_mode_override = NULL
FROM class_sections section
WHERE section.id = schedule.class_section_id
  AND schedule.delivery_mode_override = section.delivery_mode;

ALTER TABLE class_schedules
    ALTER COLUMN delivery_mode_override DROP NOT NULL;

ALTER TABLE class_schedules
    RENAME COLUMN recording_sync_status TO recording_status;

ALTER TABLE class_schedules
    DROP COLUMN meeting_url,
    DROP COLUMN lark_meeting_url,
    DROP COLUMN lark_calendar_id,
    DROP COLUMN lark_event_id,
    DROP COLUMN lark_meeting_id,
    DROP COLUMN lark_meeting_no,
    DROP COLUMN lark_reserve_id,
    DROP COLUMN lark_empty_since,
    DROP COLUMN lark_sync_status,
    DROP COLUMN lark_sync_error,
    DROP COLUMN lark_synced_at,
    DROP COLUMN lark_meeting_status,
    DROP COLUMN recording_provider,
    DROP COLUMN recording_duration_ms,
    DROP COLUMN recording_published_at,
    DROP COLUMN recording_expires_at,
    DROP COLUMN schedule_type,
    DROP COLUMN locked;

ALTER TABLE class_schedules
    DROP CONSTRAINT IF EXISTS class_schedules_status_check,
    ADD CONSTRAINT ck_class_schedules_status
        CHECK (status IN ('SCHEDULED', 'OPEN', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    ADD CONSTRAINT ck_class_schedules_special_content
        CHECK (course_lesson_id IS NOT NULL OR NULLIF(BTRIM(session_content), '') IS NOT NULL),
    ADD CONSTRAINT ck_class_schedules_recording_status
        CHECK (recording_status IN ('NOT_AVAILABLE', 'PROCESSING', 'READY', 'FAILED'));

ALTER TABLE class_sections
    DROP COLUMN virtual_meeting_owner_id,
    DROP COLUMN default_lark_meeting_url,
    DROP COLUMN lark_meeting_status,
    DROP COLUMN recording_url,
    DROP COLUMN recording_visible;

ALTER TABLE users
    DROP COLUMN IF EXISTS lark_open_id;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM class_sections
        WHERE google_meet_url IS NOT NULL
          AND google_meet_url !~* '^https://meet\.google\.com/'
    ) THEN
        RAISE EXCEPTION 'V18 found a non-Google URL in class_sections.google_meet_url';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM class_schedules schedule
        JOIN class_sections section ON section.id = schedule.class_section_id
        JOIN course_lessons lesson ON lesson.id = schedule.course_lesson_id
        JOIN course_units unit ON unit.id = lesson.course_unit_id
        WHERE unit.instructor_led_course_id <> section.instructor_led_course_id
    ) THEN
        RAISE EXCEPTION 'V18 found a cross-course ClassSchedule/CourseLesson mapping';
    END IF;
END $$;
