-- Category ordering is no longer user-configurable.
ALTER TABLE course_categories
    DROP COLUMN IF EXISTS display_order;
