-- Complete Slice 6 ownership and schedule semantics without rewriting applied V7.

-- Recover the six known demo/showcase sections from deterministic seeder slugs.
UPDATE class_sections cs
SET instructor_led_course_id = ilc.id
FROM packages p
JOIN instructor_led_courses ilc ON ilc.slug = CASE
    WHEN p.slug IN (
        'center-sheet-class-31',
        'ielts-foundation-offline',
        'ielts-registration-pipeline',
        'ielts-foundation-completed'
    ) THEN 'center-sheet-ielts-4skills'
    WHEN p.slug = 'ielts-speaking-live' THEN 'center-sheet-ielts-live'
    WHEN p.slug = 'toeic-communication-live' THEN 'center-sheet-toeic-lr'
END
WHERE cs.package_id = p.id
  AND cs.instructor_led_course_id IS NULL
  AND p.slug IN (
      'center-sheet-class-31',
      'ielts-foundation-offline',
      'ielts-speaking-live',
      'toeic-communication-live',
      'ielts-registration-pipeline',
      'ielts-foundation-completed'
  );

ALTER TABLE class_sections
    ALTER COLUMN instructor_led_course_id SET NOT NULL;

UPDATE class_enrollments ce
SET agreed_tuition_fee_vnd = COALESCE(ce.tuition_amount_due, cs.tuition_fee_vnd)
FROM class_sections cs
WHERE cs.id = ce.class_section_id
  AND ce.agreed_tuition_fee_vnd IS NULL;

ALTER TABLE class_enrollments
    ALTER COLUMN agreed_tuition_fee_vnd SET NOT NULL;

ALTER TABLE class_schedules
    ADD COLUMN IF NOT EXISTS schedule_type varchar(30);

UPDATE class_schedules
SET schedule_type = CASE
    WHEN course_lesson_id IS NOT NULL THEN 'LESSON'
    WHEN status = 'MAKEUP' THEN 'MAKEUP'
    ELSE 'OTHER'
END
WHERE schedule_type IS NULL;

ALTER TABLE class_schedules
    ALTER COLUMN schedule_type SET NOT NULL;

ALTER TABLE class_schedules
    ADD CONSTRAINT ck_class_schedules_type
        CHECK (schedule_type IN ('LESSON', 'ORIENTATION', 'EXAM', 'MAKEUP', 'OTHER')),
    ADD CONSTRAINT ck_class_schedules_lesson_required
        CHECK (schedule_type <> 'LESSON' OR course_lesson_id IS NOT NULL);

DO $$
DECLARE
    v_missing_owner bigint;
    v_missing_enrollment_fee bigint;
    v_cross_course bigint;
    v_unique_lesson_constraint bigint;
BEGIN
    SELECT COUNT(*) INTO v_missing_owner
    FROM class_sections
    WHERE instructor_led_course_id IS NULL;

    SELECT COUNT(*) INTO v_missing_enrollment_fee
    FROM class_enrollments
    WHERE agreed_tuition_fee_vnd IS NULL;

    SELECT COUNT(*) INTO v_cross_course
    FROM class_schedules schedule
    JOIN class_sections section ON section.id = schedule.class_section_id
    JOIN course_lessons lesson ON lesson.id = schedule.course_lesson_id
    JOIN course_units unit ON unit.id = lesson.course_unit_id
    WHERE section.instructor_led_course_id <> unit.instructor_led_course_id;

    SELECT COUNT(*) INTO v_unique_lesson_constraint
    FROM pg_constraint constraint_row
    WHERE constraint_row.conrelid = 'class_schedules'::regclass
      AND constraint_row.contype = 'u'
      AND pg_get_constraintdef(constraint_row.oid) ILIKE '%course_lesson_id%';

    IF v_missing_owner > 0 OR v_missing_enrollment_fee > 0
       OR v_cross_course > 0 OR v_unique_lesson_constraint > 0 THEN
        RAISE EXCEPTION
            'Slice 6 completion failed: owner=%, enrollment_fee=%, cross_course=%, unique_lesson=%',
            v_missing_owner, v_missing_enrollment_fee, v_cross_course, v_unique_lesson_constraint;
    END IF;
END $$;
