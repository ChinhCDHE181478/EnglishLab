-- V38 targeted the non-existent table name "classroom_enrollments".
-- Remove the same deprecated columns from the canonical table.
ALTER TABLE IF EXISTS class_enrollments
    DROP COLUMN IF EXISTS hold_spot CASCADE,
    DROP COLUMN IF EXISTS tuition_deposit_paid CASCADE,
    DROP COLUMN IF EXISTS confirmed_at CASCADE,
    DROP COLUMN IF EXISTS confirmed_by_id CASCADE,
    DROP COLUMN IF EXISTS quiz_score CASCADE,
    DROP COLUMN IF EXISTS participation_score CASCADE,
    DROP COLUMN IF EXISTS tuition_settlement_type CASCADE,
    DROP COLUMN IF EXISTS tuition_settlement_note CASCADE,
    DROP COLUMN IF EXISTS tuition_settlement_status CASCADE,
    DROP COLUMN IF EXISTS tuition_settlement_resolved_at CASCADE,
    DROP COLUMN IF EXISTS tuition_settlement_resolved_by_id CASCADE,
    DROP COLUMN IF EXISTS tuition_settlement_resolution_note CASCADE;
