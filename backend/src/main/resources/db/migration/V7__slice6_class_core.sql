-- Slice 6: Class Core renames (Revision 4.2).
-- classroom_offerings → class_sections, classroom_sessions → class_schedules,
-- classroom_enrollments → class_enrollments, course_enrollment_requests → course_registration_requests,
-- classroom_rooms → rooms.
-- Absorb campus address into rooms; keep classroom_campuses until Slice 10.
-- KEEP legacy TP/CP/package FK columns until Slice 10.

-- ---------------------------------------------------------------------------
-- 1. class_sections (from classroom_offerings)
-- ---------------------------------------------------------------------------
ALTER TABLE classroom_offerings
    ADD COLUMN IF NOT EXISTS code varchar(120),
    ADD COLUMN IF NOT EXISTS name varchar(180),
    ADD COLUMN IF NOT EXISTS tuition_fee_vnd numeric(12, 2),
    ADD COLUMN IF NOT EXISTS actual_end_date date;

UPDATE classroom_offerings o
SET
    code = COALESCE(NULLIF(TRIM(p.slug), ''), 'CS-' || o.id::text),
    name = COALESCE(NULLIF(TRIM(p.title), ''), 'Class ' || o.id::text),
    tuition_fee_vnd = COALESCE(NULLIF(p.sale_price, 0), p.price, 0)
FROM packages p
WHERE p.id = o.package_id
  AND (o.code IS NULL OR o.name IS NULL OR o.tuition_fee_vnd IS NULL);

ALTER TABLE classroom_offerings
    ALTER COLUMN code SET NOT NULL,
    ALTER COLUMN name SET NOT NULL,
    ALTER COLUMN tuition_fee_vnd SET NOT NULL;

ALTER TABLE classroom_offerings RENAME COLUMN max_capacity TO capacity;
ALTER TABLE classroom_offerings RENAME COLUMN end_date TO planned_end_date;
ALTER TABLE classroom_offerings RENAME COLUMN default_room_id TO regular_room_id;

ALTER TABLE classroom_offerings RENAME TO class_sections;

CREATE UNIQUE INDEX IF NOT EXISTS uk_class_sections_code ON class_sections (code);

-- ---------------------------------------------------------------------------
-- 2. Rename classroom_offering_id → class_section_id (all dependents)
-- ---------------------------------------------------------------------------
ALTER TABLE classroom_sessions RENAME COLUMN classroom_offering_id TO class_section_id;
ALTER TABLE classroom_enrollments RENAME COLUMN classroom_offering_id TO class_section_id;
ALTER TABLE classroom_announcements RENAME COLUMN classroom_offering_id TO class_section_id;
ALTER TABLE classroom_change_requests RENAME COLUMN classroom_offering_id TO class_section_id;
ALTER TABLE classroom_homework RENAME COLUMN classroom_offering_id TO class_section_id;
ALTER TABLE classroom_materials RENAME COLUMN classroom_offering_id TO class_section_id;
ALTER TABLE classroom_operation_records RENAME COLUMN classroom_offering_id TO class_section_id;
ALTER TABLE classroom_practice_attempt_history RENAME COLUMN classroom_offering_id TO class_section_id;
ALTER TABLE classroom_quizzes RENAME COLUMN classroom_offering_id TO class_section_id;
ALTER TABLE classroom_syllabus_items RENAME COLUMN classroom_offering_id TO class_section_id;
ALTER TABLE classroom_teacher_assignments RENAME COLUMN classroom_offering_id TO class_section_id;
ALTER TABLE user_auxiliary_records RENAME COLUMN classroom_offering_id TO class_section_id;

-- ---------------------------------------------------------------------------
-- 3. class_schedules (from classroom_sessions)
-- ---------------------------------------------------------------------------
ALTER TABLE classroom_sessions
    ADD COLUMN IF NOT EXISTS meeting_url varchar(700);

UPDATE classroom_sessions
SET meeting_url = lark_meeting_url
WHERE meeting_url IS NULL
  AND lark_meeting_url IS NOT NULL;

-- classroom_session_id on dependents
ALTER TABLE classroom_operation_records RENAME COLUMN classroom_session_id TO class_schedule_id;
ALTER TABLE classroom_teacher_assignments RENAME COLUMN classroom_session_id TO class_schedule_id;

ALTER TABLE classroom_sessions RENAME TO class_schedules;

-- ---------------------------------------------------------------------------
-- 4. class_enrollments
-- ---------------------------------------------------------------------------
ALTER TABLE classroom_enrollments
    ADD COLUMN IF NOT EXISTS agreed_tuition_fee_vnd numeric(12, 2);

UPDATE classroom_enrollments
SET agreed_tuition_fee_vnd = tuition_amount_due
WHERE agreed_tuition_fee_vnd IS NULL
  AND tuition_amount_due IS NOT NULL;

ALTER TABLE classroom_enrollments RENAME TO class_enrollments;

-- ---------------------------------------------------------------------------
-- 5. course_registration_requests
-- ---------------------------------------------------------------------------
ALTER TABLE course_enrollment_requests RENAME TO course_registration_requests;

ALTER TABLE course_registration_requests
    ADD COLUMN IF NOT EXISTS preferred_class_section_id bigint;

ALTER TABLE course_registration_requests
    ADD COLUMN IF NOT EXISTS assigned_class_section_id bigint;

UPDATE course_registration_requests
SET preferred_class_section_id = requested_classroom_id
WHERE preferred_class_section_id IS NULL
  AND requested_classroom_id IS NOT NULL;

UPDATE course_registration_requests
SET assigned_class_section_id = assigned_classroom_id
WHERE assigned_class_section_id IS NULL
  AND assigned_classroom_id IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_crr_preferred_class_section'
    ) THEN
        ALTER TABLE course_registration_requests
            ADD CONSTRAINT fk_crr_preferred_class_section
            FOREIGN KEY (preferred_class_section_id) REFERENCES class_sections(id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_crr_assigned_class_section'
    ) THEN
        ALTER TABLE course_registration_requests
            ADD CONSTRAINT fk_crr_assigned_class_section
            FOREIGN KEY (assigned_class_section_id) REFERENCES class_sections(id);
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 6. rooms (from classroom_rooms) — absorb campus location
-- ---------------------------------------------------------------------------
ALTER TABLE classroom_rooms
    ADD COLUMN IF NOT EXISTS location_name varchar(160),
    ADD COLUMN IF NOT EXISTS location_address varchar(500);

UPDATE classroom_rooms r
SET
    location_name = COALESCE(r.location_name, c.name),
    location_address = COALESCE(r.location_address, c.address)
FROM classroom_campuses c
WHERE c.id = r.campus_id;

ALTER TABLE classroom_rooms RENAME TO rooms;

-- ---------------------------------------------------------------------------
-- 7. Reconciliation
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    v_sections bigint;
    v_schedules bigint;
    v_enrollments bigint;
    v_requests bigint;
    v_rooms bigint;
    v_orphan_schedule bigint;
    v_orphan_enrollment bigint;
    v_sections_no_code bigint;
    v_sections_no_tuition bigint;
BEGIN
    SELECT COUNT(*) INTO v_sections FROM class_sections;
    SELECT COUNT(*) INTO v_schedules FROM class_schedules;
    SELECT COUNT(*) INTO v_enrollments FROM class_enrollments;
    SELECT COUNT(*) INTO v_requests FROM course_registration_requests;
    SELECT COUNT(*) INTO v_rooms FROM rooms;

    IF v_sections <> 39 OR v_schedules <> 1123 OR v_enrollments <> 540
       OR v_requests <> 37 OR v_rooms <> 12 THEN
        RAISE EXCEPTION
            'Slice 6 row-count mismatch: sections=%, schedules=%, enrollments=%, requests=%, rooms=%',
            v_sections, v_schedules, v_enrollments, v_requests, v_rooms;
    END IF;

    SELECT COUNT(*) INTO v_orphan_schedule
    FROM class_schedules s
    LEFT JOIN class_sections sec ON sec.id = s.class_section_id
    WHERE sec.id IS NULL;

    SELECT COUNT(*) INTO v_orphan_enrollment
    FROM class_enrollments e
    LEFT JOIN class_sections sec ON sec.id = e.class_section_id
    WHERE sec.id IS NULL;

    SELECT COUNT(*) INTO v_sections_no_code
    FROM class_sections WHERE code IS NULL OR name IS NULL;

    SELECT COUNT(*) INTO v_sections_no_tuition
    FROM class_sections WHERE tuition_fee_vnd IS NULL;

    IF v_orphan_schedule > 0 OR v_orphan_enrollment > 0
       OR v_sections_no_code > 0 OR v_sections_no_tuition > 0 THEN
        RAISE EXCEPTION
            'Slice 6 orphan/null: schedules=%, enrollments=%, sections_no_code=%, sections_no_tuition=%',
            v_orphan_schedule, v_orphan_enrollment, v_sections_no_code, v_sections_no_tuition;
    END IF;
END $$;
