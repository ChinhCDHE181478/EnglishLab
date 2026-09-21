ALTER TABLE classroom_change_requests
    DROP CONSTRAINT IF EXISTS classroom_change_requests_request_type_check;

UPDATE classroom_change_requests
SET request_type = 'RECREATE_GOOGLE_MEET'
WHERE request_type = 'UPDATE_LARK_LINK';

ALTER TABLE classroom_change_requests
    ADD CONSTRAINT classroom_change_requests_request_type_check
        CHECK (request_type IN (
            'RESCHEDULE_SESSION',
            'CHANGE_ROOM',
            'CHANGE_TEACHER',
            'CANCEL_SESSION',
            'CREATE_MAKEUP_SESSION',
            'TRANSFER_STUDENT',
            'TRANSFER_CLASS',
            'RECREATE_GOOGLE_MEET',
            'SUSPEND_STUDENT',
            'RESUME_STUDENT'
        ));

ALTER TABLE class_enrollments
    DROP CONSTRAINT IF EXISTS classroom_enrollments_registration_status_check;

ALTER TABLE class_enrollments
    ADD CONSTRAINT classroom_enrollments_registration_status_check
        CHECK (registration_status IN (
            'PENDING_CONFIRMATION',
            'PENDING_TUITION_PAYMENT',
            'DEPOSIT_PAID',
            'PARTIALLY_PAID',
            'FULLY_PAID',
            'ASSIGNED',
            'SUSPENDED',
            'WAITLIST',
            'REJECTED',
            'CANCELLED'
        ));
