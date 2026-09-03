-- ======================================================
-- V38: Remove deprecated fields and quiz tables
-- ======================================================

-- 1. placement_test_attempts: remove fraud detection field
ALTER TABLE placement_test_attempts
    DROP COLUMN IF EXISTS fraud_suspected CASCADE;

-- 2. course_enrollment_requests: remove offline test flow + campus fields
ALTER TABLE course_enrollment_requests
    DROP COLUMN IF EXISTS desired_class_code CASCADE,
    DROP COLUMN IF EXISTS campus_preference CASCADE,
    DROP COLUMN IF EXISTS test_appointment_at CASCADE,
    DROP COLUMN IF EXISTS test_location CASCADE,
    DROP COLUMN IF EXISTS test_completed_at CASCADE;

-- 3. classroom_enrollments: remove unused fields
ALTER TABLE classroom_enrollments
    DROP COLUMN IF EXISTS hold_spot CASCADE,
    DROP COLUMN IF EXISTS tuition_deposit_paid CASCADE,
    DROP COLUMN IF EXISTS confirmed_at CASCADE,
    DROP COLUMN IF EXISTS confirmed_by_id CASCADE,
    DROP COLUMN IF EXISTS quiz_score CASCADE,
    DROP COLUMN IF EXISTS participation_score CASCADE;

-- 3b. classroom_enrollments: remove tuition settlement fields
ALTER TABLE classroom_enrollments
    DROP COLUMN IF EXISTS tuition_settlement_type CASCADE,
    DROP COLUMN IF EXISTS tuition_settlement_note CASCADE,
    DROP COLUMN IF EXISTS tuition_settlement_status CASCADE,
    DROP COLUMN IF EXISTS tuition_settlement_resolved_at CASCADE,
    DROP COLUMN IF EXISTS tuition_settlement_resolved_by_id CASCADE,
    DROP COLUMN IF EXISTS tuition_settlement_resolution_note CASCADE;

-- 4. classroom_attendance_records: remove virtual tracking fields
ALTER TABLE classroom_attendance_records
    DROP COLUMN IF EXISTS join_time CASCADE,
    DROP COLUMN IF EXISTS leave_time CASCADE,
    DROP COLUMN IF EXISTS duration_minutes CASCADE,
    DROP COLUMN IF EXISTS provider_participant_key CASCADE,
    DROP COLUMN IF EXISTS provider_participant_active CASCADE;

-- 5. Drop classroom quiz tables (FK order matters, CASCADE handles lingering dependencies)
DROP TABLE IF EXISTS classroom_quiz_attempts CASCADE;
DROP TABLE IF EXISTS classroom_quiz_questions CASCADE;
DROP TABLE IF EXISTS classroom_quizzes CASCADE;
