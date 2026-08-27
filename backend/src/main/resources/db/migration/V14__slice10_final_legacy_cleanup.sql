-- ============================================================================
-- V14__slice10_final_legacy_cleanup.sql
-- EnglishLab Database Refactor: Final Slice 10 Legacy Schema Cleanup
-- Target: Exactly 59 Business Tables + flyway_schema_history (60 physical tables)
-- ============================================================================

-- ----------------------------------------------------------------------------
-- STEP 1: Pre-cleanup Migration Integrity Validations
-- ----------------------------------------------------------------------------
DO $$
BEGIN
    -- Validate content_bank_items migration coverage
    IF EXISTS (
        SELECT 1 FROM assessment_bank_items a
        LEFT JOIN content_bank_legacy_id_map m ON m.legacy_type = 'ASSESSMENT' AND m.legacy_id = a.id
        WHERE m.content_bank_item_id IS NULL
    ) THEN
        RAISE EXCEPTION 'V14 legacy cleanup failed: unmigrated assessment_bank_items';
    END IF;

    IF EXISTS (
        SELECT 1 FROM assessment_rubrics r
        LEFT JOIN content_bank_legacy_id_map m ON m.legacy_type = 'RUBRIC' AND m.legacy_id = r.id
        WHERE m.content_bank_item_id IS NULL
    ) THEN
        RAISE EXCEPTION 'V14 legacy cleanup failed: unmigrated assessment_rubrics';
    END IF;

    IF EXISTS (
        SELECT 1 FROM exercise_bank_items e
        LEFT JOIN content_bank_legacy_id_map m ON m.legacy_type = 'EXERCISE' AND m.legacy_id = e.id
        WHERE m.content_bank_item_id IS NULL
    ) THEN
        RAISE EXCEPTION 'V14 legacy cleanup failed: unmigrated exercise_bank_items';
    END IF;

    IF EXISTS (
        SELECT 1 FROM flashcard_sets f
        LEFT JOIN content_bank_legacy_id_map m ON m.legacy_type = 'FLASHCARD' AND m.legacy_id = f.id
        WHERE m.content_bank_item_id IS NULL
    ) THEN
        RAISE EXCEPTION 'V14 legacy cleanup failed: unmigrated flashcard_sets';
    END IF;

    IF EXISTS (
        SELECT 1 FROM placement_test_definitions p
        LEFT JOIN content_bank_legacy_id_map m ON m.legacy_type = 'PLACEMENT_TEST' AND m.legacy_id = p.id
        WHERE m.content_bank_item_id IS NULL
    ) THEN
        RAISE EXCEPTION 'V14 legacy cleanup failed: unmigrated placement_test_definitions';
    END IF;

    -- Validate discussion threads and replies migration coverage
    IF EXISTS (
        SELECT 1 FROM course_discussion_threads t
        LEFT JOIN course_discussion_post_id_map m ON m.legacy_kind = 'THREAD' AND m.legacy_id = t.id
        WHERE m.post_id IS NULL
    ) THEN
        RAISE EXCEPTION 'V14 legacy cleanup failed: unmigrated course_discussion_threads';
    END IF;

    IF EXISTS (
        SELECT 1 FROM course_discussion_replies r
        LEFT JOIN course_discussion_post_id_map m ON m.legacy_kind = 'REPLY' AND m.legacy_id = r.id
        WHERE m.post_id IS NULL
    ) THEN
        RAISE EXCEPTION 'V14 legacy cleanup failed: unmigrated course_discussion_replies';
    END IF;

    -- Validate instructor_led_courses migration coverage
    IF EXISTS (
        SELECT 1 FROM training_programs tp
        LEFT JOIN instructor_led_course_id_map m ON m.legacy_kind = 'TRAINING_PROGRAM' AND m.legacy_id = tp.id
        WHERE m.instructor_led_course_id IS NULL
    ) THEN
        RAISE EXCEPTION 'V14 legacy cleanup failed: unmigrated training_programs';
    END IF;

    IF EXISTS (
        SELECT 1 FROM curriculum_programs cp
        LEFT JOIN instructor_led_course_id_map m ON m.legacy_kind = 'CURRICULUM_PROGRAM' AND m.legacy_id = cp.id
        WHERE m.instructor_led_course_id IS NULL
    ) THEN
        RAISE EXCEPTION 'V14 legacy cleanup failed: unmigrated curriculum_programs';
    END IF;
END $$;

-- ----------------------------------------------------------------------------
-- STEP 2: Retarget Active Table Foreign Keys from Legacy to Canonical Tables
-- ----------------------------------------------------------------------------

-- 2a. classroom_proposals -> instructor_led_courses
ALTER TABLE classroom_proposals
    DROP CONSTRAINT IF EXISTS fkdpmu7eeye9fxhnwxfp747gi8a;

UPDATE classroom_proposals cp
SET course_offering_id = m.instructor_led_course_id
FROM instructor_led_course_id_map m
WHERE m.legacy_kind = 'TRAINING_PROGRAM'
  AND m.legacy_id = cp.course_offering_id;

ALTER TABLE classroom_proposals
    ADD CONSTRAINT fk_classroom_proposals_course_offering
    FOREIGN KEY (course_offering_id) REFERENCES instructor_led_courses(id);

-- 2b. course_registration_requests -> instructor_led_courses
ALTER TABLE course_registration_requests
    DROP CONSTRAINT IF EXISTS fkoscgt31plv0156uvlijwek6qf;

UPDATE course_registration_requests cr
SET course_offering_id = m.instructor_led_course_id
FROM instructor_led_course_id_map m
WHERE m.legacy_kind = 'TRAINING_PROGRAM'
  AND m.legacy_id = cr.course_offering_id;

ALTER TABLE course_registration_requests
    ADD CONSTRAINT fk_course_registration_requests_course_offering
    FOREIGN KEY (course_offering_id) REFERENCES instructor_led_courses(id);

-- ----------------------------------------------------------------------------
-- STEP 3: Drop Legacy Foreign Key Columns from Active Business Tables
-- ----------------------------------------------------------------------------

-- classroom_homework
ALTER TABLE classroom_homework
    DROP COLUMN IF EXISTS assessment_bank_item_id CASCADE,
    DROP COLUMN IF EXISTS curriculum_unit_id CASCADE,
    DROP COLUMN IF EXISTS rubric_id CASCADE;

-- class_resources
ALTER TABLE class_resources
    DROP COLUMN IF EXISTS curriculum_unit_id CASCADE;

-- class_sections
ALTER TABLE class_sections
    DROP CONSTRAINT IF EXISTS ukhlrmwox78y07g3nj7gl9fk8fy,
    DROP COLUMN IF EXISTS curriculum_program_id CASCADE,
    DROP COLUMN IF EXISTS package_id CASCADE,
    DROP COLUMN IF EXISTS training_program_id CASCADE;

-- classroom_practice_attempt_history
ALTER TABLE classroom_practice_attempt_history
    ALTER COLUMN exercise_content_bank_item_id SET NOT NULL;

ALTER TABLE classroom_practice_attempt_history
    DROP COLUMN IF EXISTS exercise_id CASCADE;

-- class_schedules
ALTER TABLE class_schedules
    DROP COLUMN IF EXISTS curriculum_session_plan_id CASCADE;

-- course_assessments
ALTER TABLE course_assessments
    DROP COLUMN IF EXISTS assessment_bank_item_id CASCADE,
    DROP COLUMN IF EXISTS rubric_id CASCADE;

-- mock_test_attempts
ALTER TABLE mock_test_attempts
    ALTER COLUMN assessment_content_bank_item_id SET NOT NULL;

DROP INDEX IF EXISTS idx_mock_test_attempts_item_student;

ALTER TABLE mock_test_attempts
    DROP COLUMN IF EXISTS assessment_bank_item_id CASCADE;

CREATE INDEX IF NOT EXISTS idx_mock_test_attempts_cbi_student
    ON mock_test_attempts (assessment_content_bank_item_id, student_id, submitted_at DESC);

-- online_courses
ALTER TABLE online_courses
    DROP CONSTRAINT IF EXISTS ukd6mswl5baw4ky3npro187dqbk,
    DROP COLUMN IF EXISTS package_id CASCADE;

-- online_course_enrollments
ALTER TABLE online_course_enrollments
    DROP CONSTRAINT IF EXISTS uk_package_enrollment_user_package,
    DROP COLUMN IF EXISTS package_id CASCADE;

-- ----------------------------------------------------------------------------
-- STEP 4: Drop 26 Legacy Tables in FK-Safe Dependency Order
-- ----------------------------------------------------------------------------

DROP TABLE IF EXISTS assessment_component_records CASCADE;
DROP TABLE IF EXISTS curriculum_resource_refs CASCADE;
DROP TABLE IF EXISTS assessment_bank_items CASCADE;
DROP TABLE IF EXISTS assessment_rubrics CASCADE;
DROP TABLE IF EXISTS exercise_bank_items CASCADE;
DROP TABLE IF EXISTS flashcard_sets CASCADE;
DROP TABLE IF EXISTS placement_test_definitions CASCADE;
DROP TABLE IF EXISTS content_bank_legacy_id_map CASCADE;

DROP TABLE IF EXISTS course_discussion_replies CASCADE;
DROP TABLE IF EXISTS course_discussion_threads CASCADE;
DROP TABLE IF EXISTS course_discussion_post_id_map CASCADE;

DROP TABLE IF EXISTS course_reviews CASCADE;
DROP TABLE IF EXISTS learner_progress_records CASCADE;

DROP TABLE IF EXISTS classroom_financial_records CASCADE;
DROP TABLE IF EXISTS classroom_operation_records CASCADE;
DROP TABLE IF EXISTS classroom_syllabus_items CASCADE;
DROP TABLE IF EXISTS classroom_teacher_assignments CASCADE;

DROP TABLE IF EXISTS curriculum_session_plans CASCADE;
DROP TABLE IF EXISTS curriculum_units CASCADE;
DROP TABLE IF EXISTS training_programs CASCADE;
DROP TABLE IF EXISTS curriculum_programs CASCADE;
DROP TABLE IF EXISTS instructor_led_course_id_map CASCADE;

DROP TABLE IF EXISTS packages CASCADE;
DROP TABLE IF EXISTS package_types CASCADE;

DROP TABLE IF EXISTS user_auxiliary_records CASCADE;
DROP TABLE IF EXISTS system_backup_records CASCADE;

-- ----------------------------------------------------------------------------
-- STEP 5: Final Schema Verification (Exactly 59 Business Tables)
-- ----------------------------------------------------------------------------
DO $$
DECLARE
    v_business_table_count integer;
BEGIN
    SELECT count(*) INTO v_business_table_count
    FROM pg_tables
    WHERE schemaname = 'public'
      AND tablename != 'flyway_schema_history';

    IF v_business_table_count != 59 THEN
        RAISE EXCEPTION 'V14 cleanup verification failed: expected 59 business tables, found %', v_business_table_count;
    END IF;

    RAISE NOTICE 'V14 legacy cleanup SUCCESS: Exactly 59 business tables verified.';
END $$;
