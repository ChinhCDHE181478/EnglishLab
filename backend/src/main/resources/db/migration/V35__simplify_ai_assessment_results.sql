ALTER TABLE assessment_submissions
    DROP COLUMN ai_prompt_snapshot,
    DROP COLUMN ai_provider,
    DROP COLUMN ai_model,
    DROP COLUMN ai_raw_response;

ALTER TABLE mock_test_attempts
    ADD COLUMN ai_feedback jsonb;
