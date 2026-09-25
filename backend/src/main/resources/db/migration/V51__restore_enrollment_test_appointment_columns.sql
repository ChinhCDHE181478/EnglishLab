-- V38 dropped test appointment columns; enrollment scheduling still needs them.
ALTER TABLE course_registration_requests
    ADD COLUMN IF NOT EXISTS test_appointment_at timestamp(6) without time zone;

ALTER TABLE course_registration_requests
    ADD COLUMN IF NOT EXISTS test_location character varying(300);

ALTER TABLE course_registration_requests
    ADD COLUMN IF NOT EXISTS test_completed_at timestamp(6) without time zone;
