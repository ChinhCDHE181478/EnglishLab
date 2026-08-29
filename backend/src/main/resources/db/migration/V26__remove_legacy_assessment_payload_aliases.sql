UPDATE content_bank_items
SET payload_jsonb = (
        payload_jsonb
        || jsonb_build_object(
            'type', COALESCE(
                NULLIF(payload_jsonb ->> 'type', ''),
                NULLIF(payload_jsonb ->> 'assessmentType', ''),
                'MODULE_TEST'
            ),
            'uiConfigJson', COALESCE(
                NULLIF(payload_jsonb ->> 'uiConfigJson', ''),
                NULLIF(payload_jsonb ->> 'contentJson', ''),
                '{}'
            )
        )
    ) - 'assessmentType' - 'contentJson'
WHERE bank_type = 'ASSESSMENT'
  AND (payload_jsonb ? 'assessmentType' OR payload_jsonb ? 'contentJson');

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM content_bank_items
        WHERE bank_type = 'ASSESSMENT'
          AND (payload_jsonb ? 'assessmentType' OR payload_jsonb ? 'contentJson')
    ) THEN
        RAISE EXCEPTION 'V26 blocked: legacy assessment payload aliases remain';
    END IF;
END $$;
