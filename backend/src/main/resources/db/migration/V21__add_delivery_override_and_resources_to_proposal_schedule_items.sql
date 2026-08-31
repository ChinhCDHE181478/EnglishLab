-- V21: Add delivery_mode_override, teacher_id, room_id to classroom_proposal_schedule_items

ALTER TABLE classroom_proposal_schedule_items
    ADD COLUMN IF NOT EXISTS delivery_mode_override VARCHAR(20),
    ADD COLUMN IF NOT EXISTS teacher_id BIGINT,
    ADD COLUMN IF NOT EXISTS room_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_proposal_schedule_items_teacher'
    ) THEN
        ALTER TABLE classroom_proposal_schedule_items
            ADD CONSTRAINT fk_proposal_schedule_items_teacher
            FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_proposal_schedule_items_room'
    ) THEN
        ALTER TABLE classroom_proposal_schedule_items
            ADD CONSTRAINT fk_proposal_schedule_items_room
            FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL;
    END IF;
END $$;
