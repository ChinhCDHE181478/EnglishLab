ALTER TABLE course_discussion_posts
    DROP CONSTRAINT IF EXISTS course_discussion_posts_status_check;

ALTER TABLE course_discussion_posts
    ADD CONSTRAINT course_discussion_posts_status_check
        CHECK (status IN ('OPEN', 'RESOLVED', 'PENDING_REVIEW', 'HIDDEN', 'DELETED'));
