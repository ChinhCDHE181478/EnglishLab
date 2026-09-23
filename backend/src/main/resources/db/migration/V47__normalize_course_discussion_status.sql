-- Legacy discussion rows used ACTIVE for an open thread. The current domain
-- model uses OPEN, and Hibernate cannot deserialize the old enum value.
UPDATE course_discussion_posts
SET status = UPPER(TRIM(status))
WHERE UPPER(TRIM(status)) IN ('OPEN', 'RESOLVED', 'PENDING_REVIEW', 'HIDDEN');

UPDATE course_discussion_posts
SET status = 'OPEN'
WHERE UPPER(TRIM(status)) = 'ACTIVE';

ALTER TABLE course_discussion_posts
    DROP CONSTRAINT IF EXISTS course_discussion_posts_status_check;

ALTER TABLE course_discussion_posts
    ADD CONSTRAINT course_discussion_posts_status_check
        CHECK (status IN ('OPEN', 'RESOLVED', 'PENDING_REVIEW', 'HIDDEN'));
