-- V19: Rename regular_room_id to room_id on class_sections

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'class_sections'
          AND column_name = 'regular_room_id'
    ) THEN
        ALTER TABLE class_sections RENAME COLUMN regular_room_id TO room_id;
    END IF;
END $$;
