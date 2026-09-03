DO $$
DECLARE
    invalid_count bigint;
BEGIN
    SELECT COUNT(*) INTO invalid_count
    FROM course_assessments
    WHERE NULLIF(BTRIM(ui_config_json), '') IS NOT NULL
      AND NOT pg_input_is_valid(ui_config_json, 'jsonb');
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'course_assessments.ui_config_json contains % invalid JSON value(s)', invalid_count;
    END IF;

    SELECT COUNT(*) INTO invalid_count
    FROM course_assessments
    WHERE NULLIF(BTRIM(objective_answer_key), '') IS NOT NULL
      AND NOT pg_input_is_valid(objective_answer_key, 'jsonb');
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'course_assessments.objective_answer_key contains % invalid JSON value(s)', invalid_count;
    END IF;

    SELECT COUNT(*) INTO invalid_count
    FROM assessment_submissions
    WHERE NULLIF(BTRIM(ai_feedback_json), '') IS NOT NULL
      AND NOT pg_input_is_valid(ai_feedback_json, 'jsonb');
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'assessment_submissions.ai_feedback_json contains % invalid JSON value(s)', invalid_count;
    END IF;

    SELECT COUNT(*) INTO invalid_count
    FROM assessment_submissions
    WHERE NULLIF(BTRIM(objective_answers_json), '') IS NOT NULL
      AND NOT pg_input_is_valid(objective_answers_json, 'jsonb');
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'assessment_submissions.objective_answers_json contains % invalid JSON value(s)', invalid_count;
    END IF;

    SELECT COUNT(*) INTO invalid_count
    FROM mock_test_attempts
    WHERE NULLIF(BTRIM(objective_answers_json), '') IS NOT NULL
      AND NOT pg_input_is_valid(objective_answers_json, 'jsonb');
    IF invalid_count > 0 THEN
        RAISE EXCEPTION 'mock_test_attempts.objective_answers_json contains % invalid JSON value(s)', invalid_count;
    END IF;
END $$;

ALTER TABLE content_bank_items
    RENAME COLUMN payload_jsonb TO content_data;

ALTER TABLE course_assessments
    RENAME COLUMN ui_config_json TO assessment_config;

ALTER TABLE course_assessments
    ALTER COLUMN assessment_config TYPE jsonb
        USING NULLIF(BTRIM(assessment_config), '')::jsonb,
    ALTER COLUMN objective_answer_key TYPE jsonb
        USING NULLIF(BTRIM(objective_answer_key), '')::jsonb;

ALTER TABLE assessment_submissions
    RENAME COLUMN ai_feedback_json TO ai_feedback;

ALTER TABLE assessment_submissions
    RENAME COLUMN objective_answers_json TO objective_answers;

ALTER TABLE assessment_submissions
    ALTER COLUMN ai_feedback TYPE jsonb
        USING NULLIF(BTRIM(ai_feedback), '')::jsonb,
    ALTER COLUMN objective_answers TYPE jsonb
        USING NULLIF(BTRIM(objective_answers), '')::jsonb,
    DROP COLUMN microphone_checked,
    DROP COLUMN device_check_passed;

ALTER TABLE mock_test_attempts
    RENAME COLUMN objective_answers_json TO objective_answers;

ALTER TABLE mock_test_attempts
    ALTER COLUMN objective_answers TYPE jsonb
        USING NULLIF(BTRIM(objective_answers), '')::jsonb,
    DROP COLUMN percent;
