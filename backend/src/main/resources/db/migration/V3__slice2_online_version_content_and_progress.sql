-- Slice 2: version-owned modules/lessons + dedicated lesson/vocabulary progress.
-- KEEP learner_progress_records / user_auxiliary_records STI parents (later slices).
-- KEEP online_course_id on modules temporarily for reconciliation.

-- ---------------------------------------------------------------------------
-- A. Version ownership on modules
-- ---------------------------------------------------------------------------
ALTER TABLE course_modules
    ADD COLUMN IF NOT EXISTS online_course_version_id bigint;

UPDATE course_modules cm
SET online_course_version_id = chosen.version_id
FROM (
    SELECT DISTINCT ON (ocv.online_course_id)
           ocv.online_course_id,
           ocv.id AS version_id
    FROM online_course_versions ocv
    -- Prefer PUBLISHED so existing enrollments keep lesson IDs on the pinned published graph.
    -- Later createDraft clones modules onto a new DRAFT; do not move published rows.
    ORDER BY ocv.online_course_id,
             CASE ocv.status
                 WHEN 'PUBLISHED' THEN 0
                 WHEN 'PENDING_REVIEW' THEN 1
                 WHEN 'DRAFT' THEN 2
                 ELSE 3
             END,
             ocv.version_number DESC
) chosen
WHERE cm.online_course_id = chosen.online_course_id
  AND cm.online_course_version_id IS NULL;

-- Courses with modules but no version: create a DRAFT version shell and attach.
INSERT INTO online_course_versions (
    online_course_id, version_number, status, content_snapshot_json, assessment_ids_json,
    total_required_lessons, total_required_assessments, created_at, updated_at
)
SELECT oc.id, 1, 'DRAFT', '{}', '[]', 0, 0, NOW(), NOW()
FROM online_courses oc
WHERE EXISTS (
        SELECT 1 FROM course_modules cm
        WHERE cm.online_course_id = oc.id AND cm.online_course_version_id IS NULL
      )
  AND NOT EXISTS (
        SELECT 1 FROM online_course_versions ocv WHERE ocv.online_course_id = oc.id
      );

UPDATE course_modules cm
SET online_course_version_id = ocv.id
FROM online_course_versions ocv
WHERE cm.online_course_id = ocv.online_course_id
  AND cm.online_course_version_id IS NULL
  AND ocv.status = 'DRAFT';

ALTER TABLE course_modules
    ALTER COLUMN online_course_version_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_course_modules_version') THEN
        ALTER TABLE course_modules
            ADD CONSTRAINT fk_course_modules_version
            FOREIGN KEY (online_course_version_id) REFERENCES online_course_versions(id);
    END IF;
END $$;

-- Rename display_order → sequence_number on modules (keep data)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'course_modules' AND column_name = 'display_order'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'course_modules' AND column_name = 'sequence_number'
    ) THEN
        ALTER TABLE course_modules RENAME COLUMN display_order TO sequence_number;
    END IF;
END $$;

ALTER TABLE course_modules RENAME TO online_course_modules;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_course_module_order') THEN
        ALTER TABLE online_course_modules RENAME CONSTRAINT uk_course_module_order TO uk_online_course_module_order;
    END IF;
END $$;

ALTER TABLE online_course_modules DROP CONSTRAINT IF EXISTS uk_online_course_module_order;
ALTER TABLE online_course_modules
    ADD CONSTRAINT uk_oce_module_version_seq UNIQUE (online_course_version_id, sequence_number);

CREATE INDEX IF NOT EXISTS idx_ocm_version ON online_course_modules (online_course_version_id);

-- ---------------------------------------------------------------------------
-- B. Lessons → online_lessons
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lessons' AND column_name = 'display_order'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lessons' AND column_name = 'sequence_number'
    ) THEN
        ALTER TABLE lessons RENAME COLUMN display_order TO sequence_number;
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lessons' AND column_name = 'lesson_key'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lessons' AND column_name = 'stable_lesson_key'
    ) THEN
        ALTER TABLE lessons RENAME COLUMN lesson_key TO stable_lesson_key;
    END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lessons' AND column_name = 'transcript_segments_json'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'lessons' AND column_name = 'transcript_json'
    ) THEN
        ALTER TABLE lessons RENAME COLUMN transcript_segments_json TO transcript_json;
    END IF;
END $$;

ALTER TABLE lessons RENAME TO online_lessons;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'uk_lesson_order') THEN
        ALTER TABLE online_lessons RENAME CONSTRAINT uk_lesson_order TO uk_online_lesson_order;
    END IF;
END $$;

ALTER TABLE online_lessons DROP CONSTRAINT IF EXISTS uk_online_lesson_order;
ALTER TABLE online_lessons
    ADD CONSTRAINT uk_online_lesson_module_seq UNIQUE (module_id, sequence_number);

-- ---------------------------------------------------------------------------
-- C. Dedicated lesson_progress (+ needs_review from review flags)
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS lesson_progress (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    student_id bigint NOT NULL REFERENCES users(id),
    online_lesson_id bigint NOT NULL REFERENCES online_lessons(id),
    online_course_enrollment_id bigint NOT NULL REFERENCES online_course_enrollments(id),
    course_version_id bigint REFERENCES online_course_versions(id),
    stable_lesson_key varchar(120),
    status varchar(30) NOT NULL,
    progress_percent integer NOT NULL DEFAULT 0,
    first_accessed_at timestamp(6) without time zone,
    last_accessed_at timestamp(6) without time zone,
    completed_at timestamp(6) without time zone,
    needs_review boolean NOT NULL DEFAULT false,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    CONSTRAINT lesson_progress_status_check CHECK (
        (status)::text = ANY ((ARRAY[
            'NOT_STARTED'::character varying,
            'IN_PROGRESS'::character varying,
            'COMPLETED'::character varying
        ])::text[])
    ),
    CONSTRAINT uk_lesson_progress_enrollment_lesson UNIQUE (online_course_enrollment_id, online_lesson_id)
);

INSERT INTO lesson_progress (
    id, student_id, online_lesson_id, online_course_enrollment_id, course_version_id,
    stable_lesson_key, status, progress_percent, first_accessed_at, last_accessed_at,
    completed_at, needs_review, created_at, updated_at
) OVERRIDING SYSTEM VALUE
SELECT
    lpr.id,
    lpr.student_id,
    lpr.lesson_id,
    lpr.enrollment_id,
    lpr.course_version_id,
    lpr.lesson_key,
    lpr.lesson_progress_status,
    COALESCE(lpr.progress_percent, 0),
    lpr.last_accessed_at,
    lpr.last_accessed_at,
    lpr.completed_at,
    EXISTS (
        SELECT 1
        FROM user_auxiliary_records f
        WHERE f.record_type = 'learner_lesson_review_flags'
          AND f.user_id = lpr.student_id
          AND f.lesson_id = lpr.lesson_id
    ),
    lpr.created_at,
    lpr.updated_at
FROM learner_progress_records lpr
WHERE lpr.record_type = 'lesson_progress'
  AND NOT EXISTS (SELECT 1 FROM lesson_progress lp WHERE lp.id = lpr.id);

-- Flags without progress rows → create progress stubs (needs_review only)
INSERT INTO lesson_progress (
    student_id, online_lesson_id, online_course_enrollment_id, course_version_id,
    status, progress_percent, needs_review, created_at, updated_at
)
SELECT
    f.user_id,
    f.lesson_id,
    oce.id,
    oce.course_version_id,
    'NOT_STARTED',
    0,
    true,
    f.created_at,
    f.created_at
FROM user_auxiliary_records f
JOIN online_course_enrollments oce
  ON oce.student_id = f.user_id
 AND oce.online_course_id = f.course_id
WHERE f.record_type = 'learner_lesson_review_flags'
  AND NOT EXISTS (
        SELECT 1 FROM lesson_progress lp
        WHERE lp.online_course_enrollment_id = oce.id
          AND lp.online_lesson_id = f.lesson_id
  );

SELECT setval(
    pg_get_serial_sequence('lesson_progress', 'id'),
    COALESCE((SELECT MAX(id) FROM lesson_progress), 1),
    true
);

CREATE INDEX IF NOT EXISTS idx_lesson_progress_enrollment ON lesson_progress (online_course_enrollment_id);
CREATE INDEX IF NOT EXISTS idx_lesson_progress_lesson ON lesson_progress (online_lesson_id);

-- ---------------------------------------------------------------------------
-- D. Dedicated vocabulary_progress
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS vocabulary_progress (
    id bigint GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    student_id bigint NOT NULL REFERENCES users(id),
    online_course_id bigint NOT NULL REFERENCES online_courses(id),
    term_key varchar(220) NOT NULL,
    status varchar(30) NOT NULL,
    starred boolean NOT NULL DEFAULT false,
    last_reviewed_at timestamp(6) without time zone,
    review_count integer DEFAULT 0,
    correct_count integer DEFAULT 0,
    incorrect_count integer DEFAULT 0,
    last_result_correct boolean,
    created_at timestamp(6) without time zone,
    updated_at timestamp(6) without time zone,
    CONSTRAINT vocabulary_progress_status_check CHECK (
        (status)::text = ANY ((ARRAY[
            'NEW'::character varying,
            'LEARNING'::character varying,
            'MASTERED'::character varying
        ])::text[])
    ),
    CONSTRAINT uk_vocabulary_progress_student_course_term UNIQUE (student_id, online_course_id, term_key)
);

INSERT INTO vocabulary_progress (
    id, student_id, online_course_id, term_key, status, starred,
    last_reviewed_at, review_count, correct_count, incorrect_count,
    last_result_correct, created_at, updated_at
) OVERRIDING SYSTEM VALUE
SELECT
    lpr.id,
    lpr.student_id,
    lpr.course_id,
    lpr.term_key,
    lpr.vocabulary_status,
    COALESCE(lpr.starred, false),
    lpr.last_reviewed_at,
    COALESCE(lpr.review_count, 0),
    COALESCE(lpr.correct_count, 0),
    COALESCE(lpr.incorrect_count, 0),
    lpr.last_result_correct,
    lpr.created_at,
    lpr.updated_at
FROM learner_progress_records lpr
WHERE lpr.record_type = 'vocabulary_progress'
  AND NOT EXISTS (SELECT 1 FROM vocabulary_progress vp WHERE vp.id = lpr.id);

SELECT setval(
    pg_get_serial_sequence('vocabulary_progress', 'id'),
    COALESCE((SELECT MAX(id) FROM vocabulary_progress), 1),
    true
);

-- ---------------------------------------------------------------------------
-- E. Reconciliation
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    orphan_modules bigint;
    cross_course bigint;
    orphan_lessons bigint;
    bad_enroll_version bigint;
    bad_progress_version bigint;
    dup_progress bigint;
    legacy_lp bigint;
    target_lp bigint;
    flag_count bigint;
    needs_review_count bigint;
    legacy_vp bigint;
    target_vp bigint;
BEGIN
    SELECT count(*) INTO orphan_modules
    FROM online_course_modules WHERE online_course_version_id IS NULL;
    IF orphan_modules <> 0 THEN
        RAISE EXCEPTION 'Slice2 A failed: % modules without version', orphan_modules;
    END IF;

    SELECT count(*) INTO cross_course
    FROM online_course_modules ocm
    JOIN online_course_versions ocv ON ocv.id = ocm.online_course_version_id
    WHERE ocm.online_course_id IS NOT NULL
      AND ocm.online_course_id <> ocv.online_course_id;
    IF cross_course <> 0 THEN
        RAISE EXCEPTION 'Slice2 B failed: % modules version/course mismatch', cross_course;
    END IF;

    SELECT count(*) INTO orphan_lessons
    FROM online_lessons ol
    WHERE NOT EXISTS (SELECT 1 FROM online_course_modules ocm WHERE ocm.id = ol.module_id);
    IF orphan_lessons <> 0 THEN
        RAISE EXCEPTION 'Slice2 C failed: % lessons without module', orphan_lessons;
    END IF;

    SELECT count(*) INTO bad_enroll_version
    FROM online_course_enrollments oce
    JOIN online_course_versions ocv ON ocv.id = oce.course_version_id
    WHERE ocv.online_course_id <> oce.online_course_id;
    IF bad_enroll_version <> 0 THEN
        RAISE EXCEPTION 'Slice2 D failed: % enrollments with foreign version', bad_enroll_version;
    END IF;

    SELECT count(*) INTO bad_progress_version
    FROM lesson_progress lp
    JOIN online_course_enrollments oce ON oce.id = lp.online_course_enrollment_id
    JOIN online_lessons ol ON ol.id = lp.online_lesson_id
    JOIN online_course_modules ocm ON ocm.id = ol.module_id
    WHERE ocm.online_course_version_id IS DISTINCT FROM oce.course_version_id
      AND oce.course_version_id IS NOT NULL;
    -- Soft check: progress may reference lessons on draft while enrollment pinned to published.
    -- Only fail hard when lesson's version course differs from enrollment course.
    SELECT count(*) INTO bad_progress_version
    FROM lesson_progress lp
    JOIN online_course_enrollments oce ON oce.id = lp.online_course_enrollment_id
    JOIN online_lessons ol ON ol.id = lp.online_lesson_id
    JOIN online_course_modules ocm ON ocm.id = ol.module_id
    JOIN online_course_versions ocv ON ocv.id = ocm.online_course_version_id
    WHERE ocv.online_course_id <> oce.online_course_id;
    IF bad_progress_version <> 0 THEN
        RAISE EXCEPTION 'Slice2 E failed: % progress rows whose lesson is outside enrollment course', bad_progress_version;
    END IF;

    SELECT count(*) INTO dup_progress
    FROM (
        SELECT online_course_enrollment_id, online_lesson_id
        FROM lesson_progress
        GROUP BY 1, 2 HAVING count(*) > 1
    ) d;
    IF dup_progress <> 0 THEN
        RAISE EXCEPTION 'Slice2 F failed: % duplicate enrollment/lesson progress', dup_progress;
    END IF;

    SELECT count(*) INTO legacy_lp FROM learner_progress_records WHERE record_type = 'lesson_progress';
    SELECT count(*) INTO target_lp FROM lesson_progress WHERE id IN (
        SELECT id FROM learner_progress_records WHERE record_type = 'lesson_progress'
    );
    IF legacy_lp <> target_lp THEN
        RAISE EXCEPTION 'Slice2 G failed: legacy lesson_progress=% vs migrated=%', legacy_lp, target_lp;
    END IF;

    SELECT count(*) INTO flag_count
    FROM user_auxiliary_records WHERE record_type = 'learner_lesson_review_flags';
    SELECT count(*) INTO needs_review_count FROM lesson_progress WHERE needs_review = true;
    IF needs_review_count < flag_count THEN
        RAISE EXCEPTION 'Slice2 H failed: flags=% but needs_review rows=%', flag_count, needs_review_count;
    END IF;

    SELECT count(*) INTO legacy_vp FROM learner_progress_records WHERE record_type = 'vocabulary_progress';
    SELECT count(*) INTO target_vp FROM vocabulary_progress;
    IF legacy_vp <> target_vp THEN
        RAISE EXCEPTION 'Slice2 I failed: legacy vocabulary_progress=% vs migrated=%', legacy_vp, target_vp;
    END IF;
END $$;
