ALTER TABLE classroom_attendance_records
    ADD COLUMN IF NOT EXISTS provider_participant_key varchar(255),
    ADD COLUMN IF NOT EXISTS provider_participant_active boolean NOT NULL DEFAULT false;

CREATE UNIQUE INDEX IF NOT EXISTS uk_attendance_provider_participant
    ON classroom_attendance_records (session_id, provider_participant_key)
    WHERE provider_participant_key IS NOT NULL;

