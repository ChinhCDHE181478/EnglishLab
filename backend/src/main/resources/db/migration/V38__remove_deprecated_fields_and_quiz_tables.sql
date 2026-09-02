-- ======================================================
-- V38: Remove deprecated fields and quiz tables
-- ======================================================

-- 1. placement_test_attempts: remove fraud detection field
ALTER TABLE placement_test_attempts
    DROP COLUMN IF EXISTS fraud_suspected;

-- 2. course_enrollment_requests: remove offline test flow + campus fields
ALTER TABLE course_enrollment_requests
    DROP COLUMN IF EXISTS desired_class_code,
    DROP COLUMN IF EXISTS campus_preference,
    DROP COLUMN IF EXISTS test_appointment_at,
    DROP COLUMN IF EXISTS test_location,
    DROP COLUMN IF EXISTS test_completed_at;

-- 3. classroom_enrollments: remove unused fields
ALTER TABLE classroom_enrollments
    DROP COLUMN IF EXISTS hold_spot,
    DROP COLUMN IF EXISTS tuition_deposit_paid,
    DROP COLUMN IF EXISTS confirmed_at,
    DROP COLUMN IF EXISTS confirmed_by_id,
    DROP COLUMN IF EXISTS quiz_score,
    DROP COLUMN IF EXISTS participation_score;

-- 3b. classroom_enrollments: remove tuition settlement fields
ALTER TABLE classroom_enrollments
    DROP COLUMN IF EXISTS tuition_settlement_type,
    DROP COLUMN IF EXISTS tuition_settlement_note,
    DROP COLUMN IF EXISTS tuition_settlement_status,
    DROP COLUMN IF EXISTS tuition_settlement_resolved_at,
    DROP COLUMN IF EXISTS tuition_settlement_resolved_by_id,
    DROP COLUMN IF EXISTS tuition_settlement_resolution_note;

-- 4. classroom_attendance_records: remove virtual tracking fields
ALTER TABLE classroom_attendance_records
    DROP COLUMN IF EXISTS join_time,
    DROP COLUMN IF EXISTS leave_time,
    DROP COLUMN IF EXISTS duration_minutes,
    DROP COLUMN IF EXISTS provider_participant_key,
    DROP COLUMN IF EXISTS provider_participant_active;

-- 5. Drop classroom quiz tables (FK order matters)
DROP TABLE IF EXISTS classroom_quiz_attempts;
DROP TABLE IF EXISTS classroom_quiz_questions;
DROP TABLE IF EXISTS classroom_quizzes;
