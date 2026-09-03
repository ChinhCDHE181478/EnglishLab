DO $$
DECLARE
    unmatched_codes INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO unmatched_codes
    FROM online_courses course
    LEFT JOIN learning_paths path
        ON UPPER(BTRIM(path.code)) = UPPER(BTRIM(course.learning_path_code))
    WHERE NULLIF(BTRIM(course.learning_path_code), '') IS NOT NULL
      AND path.id IS NULL;

    IF unmatched_codes > 0 THEN
        RAISE EXCEPTION 'Cannot remove online course learning-path columns: % legacy codes have no canonical learning_paths row', unmatched_codes;
    END IF;
END $$;

WITH missing_relations AS (
    SELECT
        path.id AS learning_path_id,
        course.id AS online_course_id,
        course.learning_path_order AS legacy_order,
        ROW_NUMBER() OVER (PARTITION BY path.id ORDER BY course.id) AS fallback_offset,
        COALESCE((
            SELECT MAX(current_relation.display_order)
            FROM learning_path_courses current_relation
            WHERE current_relation.learning_path_id = path.id
        ), 0) AS current_max_order
    FROM online_courses course
    JOIN learning_paths path
        ON UPPER(BTRIM(path.code)) = UPPER(BTRIM(course.learning_path_code))
    LEFT JOIN learning_path_courses existing_relation
        ON existing_relation.learning_path_id = path.id
       AND existing_relation.online_course_id = course.id
    WHERE NULLIF(BTRIM(course.learning_path_code), '') IS NOT NULL
      AND existing_relation.id IS NULL
)
INSERT INTO learning_path_courses (learning_path_id, online_course_id, display_order)
SELECT
    learning_path_id,
    online_course_id,
    COALESCE(legacy_order, current_max_order + fallback_offset)
FROM missing_relations;

DO $$
DECLARE
    missing_relations INTEGER;
BEGIN
    SELECT COUNT(*)
    INTO missing_relations
    FROM online_courses course
    JOIN learning_paths path
        ON UPPER(BTRIM(path.code)) = UPPER(BTRIM(course.learning_path_code))
    LEFT JOIN learning_path_courses relation
        ON relation.learning_path_id = path.id
       AND relation.online_course_id = course.id
    WHERE NULLIF(BTRIM(course.learning_path_code), '') IS NOT NULL
      AND relation.id IS NULL;

    IF missing_relations > 0 THEN
        RAISE EXCEPTION 'Cannot remove online course learning-path columns: % relationships were not migrated', missing_relations;
    END IF;
END $$;

ALTER TABLE online_courses
    DROP COLUMN learning_path_code,
    DROP COLUMN learning_path_name,
    DROP COLUMN learning_path_order,
    DROP COLUMN recommended_next_course_slug;
