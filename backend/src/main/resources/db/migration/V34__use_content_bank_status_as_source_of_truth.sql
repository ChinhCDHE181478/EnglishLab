DO $$
DECLARE
    inconsistent_count bigint;
BEGIN
    SELECT COUNT(*) INTO inconsistent_count
    FROM content_bank_items
    WHERE active <> (UPPER(status) = 'PUBLISHED');

    IF inconsistent_count > 0 THEN
        RAISE EXCEPTION 'V34 blocked: % content bank row(s) have inconsistent active/status values', inconsistent_count;
    END IF;
END $$;

UPDATE course_assessments
SET progress_key = 'assessment-' || id
WHERE progress_key IS NULL OR BTRIM(progress_key) = '';

ALTER TABLE course_assessments
    ALTER COLUMN progress_key SET NOT NULL;

ALTER TABLE content_bank_items
    DROP COLUMN active;
