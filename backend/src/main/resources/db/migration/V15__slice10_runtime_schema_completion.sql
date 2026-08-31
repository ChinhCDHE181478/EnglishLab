-- Complete canonical columns required by the Revision 4.2 runtime model.
ALTER TABLE classroom_quizzes
    ADD COLUMN IF NOT EXISTS questions_jsonb jsonb NOT NULL DEFAULT '[]'::jsonb;

ALTER TABLE classroom_quiz_attempts
    ADD COLUMN IF NOT EXISTS questions_snapshot_jsonb jsonb NOT NULL DEFAULT '[]'::jsonb;

