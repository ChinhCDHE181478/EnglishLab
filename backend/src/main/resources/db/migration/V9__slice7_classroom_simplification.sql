-- Slice 7: collapse single-center infrastructure and prepare classroom state
-- to leave the legacy classroom operation record table safely.

UPDATE rooms room
SET location_name = COALESCE(room.location_name, campus.name),
    location_address = COALESCE(room.location_address, campus.address)
FROM classroom_campuses campus
WHERE room.campus_id = campus.id;

-- The two old demo rooms had no campus FK. In the approved single-center model,
-- the sole active center is their deterministic location as well.
UPDATE rooms room
SET location_name = COALESCE(room.location_name, center.name),
    location_address = COALESCE(room.location_address, center.address)
FROM (
    SELECT name, address
    FROM classroom_campuses
    WHERE active = true
    ORDER BY id
    LIMIT 1
) center
WHERE room.location_name IS NULL AND room.location_address IS NULL;

ALTER TABLE rooms DROP COLUMN campus_id CASCADE;
DROP TABLE classroom_campuses;

ALTER TABLE class_enrollments
    ADD COLUMN homework_score numeric(6,2),
    ADD COLUMN quiz_score numeric(6,2),
    ADD COLUMN attendance_percent numeric(5,2),
    ADD COLUMN participation_score numeric(6,2),
    ADD COLUMN final_result numeric(6,2),
    ADD COLUMN teacher_comment text,
    ADD COLUMN gradebook_status varchar(20) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN gradebook_updated_by_id bigint REFERENCES users(id),
    ADD COLUMN gradebook_updated_at timestamp without time zone;

UPDATE class_enrollments enrollment
SET homework_score = grade.homework_score,
    quiz_score = grade.quiz_score,
    attendance_percent = grade.attendance_percent,
    participation_score = grade.participation_score,
    final_result = grade.final_result,
    teacher_comment = grade.teacher_comment,
    gradebook_status = COALESCE(grade.gradebook_status, 'PENDING'),
    gradebook_updated_by_id = grade.updated_by_id,
    gradebook_updated_at = grade.updated_at
FROM classroom_operation_records grade
WHERE grade.record_type = 'classroom_gradebook_entries'
  AND grade.class_section_id = enrollment.class_section_id
  AND grade.student_id = enrollment.student_id;

ALTER TABLE course_registration_requests
    ADD COLUMN classroom_proposal_id bigint REFERENCES classroom_proposals(id),
    ADD COLUMN class_enrollment_id bigint REFERENCES class_enrollments(id);

UPDATE course_registration_requests request
SET classroom_proposal_id = member.proposal_id,
    class_enrollment_id = member.classroom_enrollment_id
FROM classroom_operation_records member
WHERE member.record_type = 'classroom_proposal_members'
  AND member.enrollment_request_id = request.id;

ALTER TABLE classroom_attendance_records
    ADD COLUMN dispute_reason text,
    ADD COLUMN dispute_status varchar(20),
    ADD COLUMN dispute_review_note text,
    ADD COLUMN dispute_reviewed_by_id bigint REFERENCES users(id),
    ADD COLUMN dispute_reviewed_at timestamp without time zone;

UPDATE classroom_attendance_records attendance
SET dispute_reason = dispute.dispute_reason,
    dispute_status = dispute.dispute_status,
    dispute_review_note = dispute.review_note,
    dispute_reviewed_by_id = dispute.reviewed_by_id,
    dispute_reviewed_at = dispute.reviewed_at
FROM classroom_operation_records dispute
WHERE dispute.record_type = 'classroom_attendance_disputes'
  AND dispute.attendance_id = attendance.id;

DO $$
DECLARE
    v_rooms_without_migrated_location bigint;
    v_missing_gradebook bigint;
    v_missing_proposal_members bigint;
    v_missing_disputes bigint;
BEGIN
    SELECT COUNT(*) INTO v_rooms_without_migrated_location
    FROM rooms
    WHERE location_name IS NULL AND location_address IS NULL;

    SELECT COUNT(*) INTO v_missing_gradebook
    FROM classroom_operation_records grade
    LEFT JOIN class_enrollments enrollment
      ON enrollment.class_section_id = grade.class_section_id
     AND enrollment.student_id = grade.student_id
    WHERE grade.record_type = 'classroom_gradebook_entries'
      AND enrollment.id IS NULL;

    SELECT COUNT(*) INTO v_missing_proposal_members
    FROM classroom_operation_records member
    LEFT JOIN course_registration_requests request
      ON request.id = member.enrollment_request_id
     AND request.classroom_proposal_id = member.proposal_id
    WHERE member.record_type = 'classroom_proposal_members'
      AND request.id IS NULL;

    SELECT COUNT(*) INTO v_missing_disputes
    FROM classroom_operation_records dispute
    LEFT JOIN classroom_attendance_records attendance
      ON attendance.id = dispute.attendance_id
     AND attendance.dispute_reason IS NOT DISTINCT FROM dispute.dispute_reason
    WHERE dispute.record_type = 'classroom_attendance_disputes'
      AND attendance.id IS NULL;

    IF v_rooms_without_migrated_location > 0 OR v_missing_gradebook > 0
       OR v_missing_proposal_members > 0 OR v_missing_disputes > 0 THEN
        RAISE EXCEPTION
            'Slice 7 reconciliation failed: room_location=%, gradebook=%, proposal=%, dispute=%',
            v_rooms_without_migrated_location, v_missing_gradebook,
            v_missing_proposal_members, v_missing_disputes;
    END IF;
END $$;
