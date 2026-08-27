-- V20: Add planned_session_count to course_lessons and create classroom_proposal_schedule_items table

ALTER TABLE course_lessons
    ADD COLUMN IF NOT EXISTS planned_session_count INTEGER NOT NULL DEFAULT 1;

CREATE TABLE IF NOT EXISTS classroom_proposal_schedule_items (
    id BIGSERIAL PRIMARY KEY,
    proposal_id BIGINT NOT NULL,
    sequence_number INTEGER NOT NULL,
    session_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    course_lesson_id BIGINT,
    session_content TEXT,
    note VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_proposal_schedule_items_proposal
        FOREIGN KEY (proposal_id) REFERENCES classroom_proposals(id) ON DELETE CASCADE,
    CONSTRAINT fk_proposal_schedule_items_course_lesson
        FOREIGN KEY (course_lesson_id) REFERENCES course_lessons(id) ON DELETE SET NULL
);

CREATE INDEX IF NOT EXISTS idx_proposal_schedule_items_proposal_id
    ON classroom_proposal_schedule_items(proposal_id);

CREATE INDEX IF NOT EXISTS idx_proposal_schedule_items_course_lesson_id
    ON classroom_proposal_schedule_items(course_lesson_id);
