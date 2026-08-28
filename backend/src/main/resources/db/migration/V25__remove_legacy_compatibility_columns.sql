DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM online_course_modules module
        JOIN online_course_versions version ON version.id = module.online_course_version_id
        WHERE module.online_course_version_id IS NULL
           OR module.online_course_id IS DISTINCT FROM version.online_course_id
    ) THEN
        RAISE EXCEPTION 'V25 blocked: online module ownership differs from its version';
    END IF;

    IF EXISTS (SELECT 1 FROM course_discussion_reactions WHERE post_id IS NULL) THEN
        RAISE EXCEPTION 'V25 blocked: discussion reaction without canonical post';
    END IF;

    IF EXISTS (SELECT 1 FROM course_discussion_reports WHERE post_id IS NULL) THEN
        RAISE EXCEPTION 'V25 blocked: discussion report without canonical post';
    END IF;
END $$;

UPDATE class_enrollments
SET registration_status = CASE status
    WHEN 'ENROLLED' THEN 'ASSIGNED'
    WHEN 'COMPLETED' THEN 'ASSIGNED'
    WHEN 'CANCELLED' THEN 'CANCELLED'
    WHEN 'DROPPED' THEN 'CANCELLED'
    WHEN 'TRANSFERRED' THEN 'CANCELLED'
    ELSE 'PENDING_TUITION_PAYMENT'
END
WHERE registration_status IS NULL;

ALTER TABLE online_course_modules
    DROP COLUMN IF EXISTS online_course_id;

ALTER TABLE class_enrollments
    DROP COLUMN IF EXISTS package_enrollment_id,
    DROP COLUMN IF EXISTS status;

ALTER TABLE course_discussion_reactions
    DROP CONSTRAINT IF EXISTS uk_discussion_reaction_target_user,
    ALTER COLUMN post_id SET NOT NULL,
    DROP COLUMN IF EXISTS target_type,
    DROP COLUMN IF EXISTS target_id;

DROP INDEX IF EXISTS idx_discussion_reaction_target;

ALTER TABLE course_discussion_reactions
    ADD CONSTRAINT uk_discussion_reaction_post_user UNIQUE (post_id, user_id);

CREATE INDEX IF NOT EXISTS idx_discussion_reaction_post
    ON course_discussion_reactions(post_id);

ALTER TABLE course_discussion_reports
    DROP CONSTRAINT IF EXISTS uk_discussion_report_user_target,
    ALTER COLUMN post_id SET NOT NULL,
    DROP COLUMN IF EXISTS target_type,
    DROP COLUMN IF EXISTS target_id,
    ADD CONSTRAINT uk_discussion_report_post_user UNIQUE (post_id, reporter_id);
