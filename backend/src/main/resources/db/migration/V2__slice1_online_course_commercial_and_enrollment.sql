-- Slice 1: OnlineCourse commercial backfill + OnlineCourseEnrollment rename + review merge.
-- KEEP packages, package_types, course_reviews (physical tables). Do not drop.
-- package_enrollments is RENAMED (data preserved; FKs follow) to online_course_enrollments.

-- ---------------------------------------------------------------------------
-- A. Lift LearningPackage commercial/catalog fields onto online_courses
-- ---------------------------------------------------------------------------
ALTER TABLE online_courses
    ADD COLUMN IF NOT EXISTS title varchar(180),
    ADD COLUMN IF NOT EXISTS slug varchar(220),
    ADD COLUMN IF NOT EXISTS short_description varchar(500),
    ADD COLUMN IF NOT EXISTS description text,
    ADD COLUMN IF NOT EXISTS target_score varchar(80),
    ADD COLUMN IF NOT EXISTS duration_label varchar(80),
    ADD COLUMN IF NOT EXISTS study_mode varchar(120),
    ADD COLUMN IF NOT EXISTS price numeric(12, 2),
    ADD COLUMN IF NOT EXISTS sale_price numeric(12, 2),
    ADD COLUMN IF NOT EXISTS thumbnail_url varchar(700),
    ADD COLUMN IF NOT EXISTS status varchar(30),
    ADD COLUMN IF NOT EXISTS display_order integer,
    ADD COLUMN IF NOT EXISTS featured boolean,
    ADD COLUMN IF NOT EXISTS deleted boolean,
    ADD COLUMN IF NOT EXISTS created_by_id bigint,
    ADD COLUMN IF NOT EXISTS review_note text,
    ADD COLUMN IF NOT EXISTS submitted_for_review_at timestamp(6) without time zone,
    ADD COLUMN IF NOT EXISTS reviewed_at timestamp(6) without time zone,
    ADD COLUMN IF NOT EXISTS reviewed_by_id bigint,
    ADD COLUMN IF NOT EXISTS created_at timestamp(6) without time zone,
    ADD COLUMN IF NOT EXISTS updated_at timestamp(6) without time zone;

UPDATE online_courses oc
SET
    title = p.title,
    slug = p.slug,
    short_description = p.short_description,
    description = p.description,
    target_score = p.target_score,
    duration_label = p.duration_label,
    study_mode = p.study_mode,
    price = COALESCE(p.price, 0),
    sale_price = p.sale_price,
    thumbnail_url = p.thumbnail_url,
    status = p.status,
    display_order = COALESCE(p.display_order, 0),
    featured = COALESCE(p.featured, false),
    deleted = COALESCE(p.deleted, false),
    created_by_id = p.created_by_id,
    review_note = p.review_note,
    submitted_for_review_at = p.submitted_for_review_at,
    reviewed_at = p.reviewed_at,
    reviewed_by_id = p.reviewed_by_id,
    created_at = p.created_at,
    updated_at = p.updated_at
FROM packages p
WHERE oc.package_id = p.id
  AND oc.title IS NULL;

ALTER TABLE online_courses
    ALTER COLUMN title SET NOT NULL,
    ALTER COLUMN slug SET NOT NULL,
    ALTER COLUMN price SET NOT NULL,
    ALTER COLUMN status SET NOT NULL,
    ALTER COLUMN display_order SET NOT NULL,
    ALTER COLUMN featured SET NOT NULL,
    ALTER COLUMN deleted SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_online_courses_slug'
    ) THEN
        ALTER TABLE online_courses ADD CONSTRAINT uk_online_courses_slug UNIQUE (slug);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'online_courses_status_check'
    ) THEN
        ALTER TABLE online_courses
            ADD CONSTRAINT online_courses_status_check
            CHECK ((status)::text = ANY ((ARRAY[
                'DRAFT'::character varying,
                'PENDING_REVIEW'::character varying,
                'PUBLISHED'::character varying,
                'REJECTED'::character varying,
                'ARCHIVED'::character varying
            ])::text[]));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_online_courses_created_by'
    ) THEN
        ALTER TABLE online_courses
            ADD CONSTRAINT fk_online_courses_created_by
            FOREIGN KEY (created_by_id) REFERENCES users(id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_online_courses_reviewed_by'
    ) THEN
        ALTER TABLE online_courses
            ADD CONSTRAINT fk_online_courses_reviewed_by
            FOREIGN KEY (reviewed_by_id) REFERENCES users(id);
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- B. Rename package_enrollments → online_course_enrollments (preserve row ids)
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF to_regclass('public.package_enrollments') IS NOT NULL
       AND to_regclass('public.online_course_enrollments') IS NULL THEN
        ALTER TABLE package_enrollments RENAME TO online_course_enrollments;
    END IF;
END $$;

ALTER TABLE online_course_enrollments
    ADD COLUMN IF NOT EXISTS online_course_id bigint,
    ADD COLUMN IF NOT EXISTS review_rating integer,
    ADD COLUMN IF NOT EXISTS review_comment text,
    ADD COLUMN IF NOT EXISTS reviewed_at timestamp(6) without time zone;

UPDATE online_course_enrollments oce
SET online_course_id = oc.id
FROM online_courses oc
WHERE oce.online_course_id IS NULL
  AND oce.package_id = oc.package_id;

ALTER TABLE online_course_enrollments
    ALTER COLUMN online_course_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_oce_online_course'
    ) THEN
        ALTER TABLE online_course_enrollments
            ADD CONSTRAINT fk_oce_online_course
            FOREIGN KEY (online_course_id) REFERENCES online_courses(id);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_oce_student_course'
    ) THEN
        ALTER TABLE online_course_enrollments
            ADD CONSTRAINT uk_oce_student_course UNIQUE (student_id, online_course_id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_oce_online_course_id ON online_course_enrollments (online_course_id);
CREATE INDEX IF NOT EXISTS idx_oce_course_version ON online_course_enrollments (course_version_id);

-- ---------------------------------------------------------------------------
-- C. Backfill course reviews onto enrollments
-- ---------------------------------------------------------------------------
UPDATE online_course_enrollments oce
SET
    review_rating = cr.rating,
    review_comment = cr.comment,
    reviewed_at = COALESCE(cr.updated_at, cr.created_at)
FROM course_reviews cr
WHERE cr.student_id = oce.student_id
  AND cr.course_id = oce.online_course_id
  AND oce.review_rating IS NULL;

-- ---------------------------------------------------------------------------
-- D. Reconciliation — fail migration if unsafe
-- ---------------------------------------------------------------------------
DO $$
DECLARE
    online_pkg_count bigint;
    online_course_count bigint;
    dup_enrollments bigint;
    review_count bigint;
    migrated_review_count bigint;
    orphan_reviews bigint;
    bad_versions bigint;
BEGIN
    SELECT count(*) INTO online_pkg_count
    FROM packages p
    JOIN package_types pt ON pt.id = p.package_type_id
    WHERE pt.code = 'ONLINE_COURSE'
      AND EXISTS (SELECT 1 FROM online_courses oc WHERE oc.package_id = p.id);

    SELECT count(*) INTO online_course_count FROM online_courses;

    IF online_pkg_count <> online_course_count THEN
        RAISE EXCEPTION
            'Slice1 reconcile A failed: ONLINE_COURSE packages with course=% vs online_courses=%',
            online_pkg_count, online_course_count;
    END IF;

    IF EXISTS (
        SELECT 1 FROM online_courses WHERE title IS NULL OR slug IS NULL OR status IS NULL
    ) THEN
        RAISE EXCEPTION 'Slice1 reconcile A failed: online_courses missing commercial fields';
    END IF;

    SELECT count(*) INTO dup_enrollments
    FROM (
        SELECT student_id, online_course_id
        FROM online_course_enrollments
        GROUP BY student_id, online_course_id
        HAVING count(*) > 1
    ) d;
    IF dup_enrollments <> 0 THEN
        RAISE EXCEPTION
            'Slice1 reconcile B failed: % duplicate (student, online_course) enrollment groups',
            dup_enrollments;
    END IF;

    SELECT count(*) INTO review_count FROM course_reviews;
    SELECT count(*) INTO migrated_review_count
    FROM online_course_enrollments WHERE review_rating IS NOT NULL;
    IF review_count <> migrated_review_count THEN
        RAISE EXCEPTION
            'Slice1 reconcile C failed: course_reviews=% vs migrated review_rating rows=%',
            review_count, migrated_review_count;
    END IF;

    SELECT count(*) INTO orphan_reviews
    FROM course_reviews cr
    WHERE NOT EXISTS (
        SELECT 1
        FROM online_course_enrollments oce
        WHERE oce.student_id = cr.student_id
          AND oce.online_course_id = cr.course_id
          AND oce.review_rating = cr.rating
    );
    IF orphan_reviews <> 0 THEN
        RAISE EXCEPTION
            'Slice1 reconcile C failed: % course_reviews without exact enrollment match',
            orphan_reviews;
    END IF;

    SELECT count(*) INTO bad_versions
    FROM online_course_enrollments oce
    JOIN online_course_versions ocv ON ocv.id = oce.course_version_id
    WHERE ocv.online_course_id <> oce.online_course_id;
    IF bad_versions <> 0 THEN
        RAISE EXCEPTION
            'Slice1 reconcile D failed: % enrollments with course_version of another OnlineCourse',
            bad_versions;
    END IF;
END $$;
