-- V22: Seed default learning paths if empty
INSERT INTO learning_paths (code, name, exam_category, target_band, target_score, discount_percent, minimum_courses_for_discount)
VALUES
  ('LP-IELTS-65', 'Lộ trình IELTS Chinh phục 6.5+ (All-in-One)', 'IELTS', 6.5, NULL, 25, 2),
  ('LP-TOEIC-650', 'Lộ trình TOEIC Toàn diện 650+', 'TOEIC', NULL, 650, 20, 2),
  ('LP-COMM-WORK', 'Lộ trình Tiếng Anh Giao tiếp & Nền tảng Doanh nghiệp', 'GENERAL_ENGLISH', NULL, NULL, 15, 2)
ON CONFLICT (code) DO NOTHING;

-- Attach IELTS courses
INSERT INTO learning_path_courses (learning_path_id, online_course_id, display_order)
SELECT lp.id, oc.id, 1
FROM learning_paths lp, online_courses oc
WHERE lp.code = 'LP-IELTS-65' AND oc.slug = 'center-sheet-ielts-listening'
ON CONFLICT (learning_path_id, online_course_id) DO NOTHING;

INSERT INTO learning_path_courses (learning_path_id, online_course_id, display_order)
SELECT lp.id, oc.id, 2
FROM learning_paths lp, online_courses oc
WHERE lp.code = 'LP-IELTS-65' AND oc.slug = 'center-sheet-ielts-reading'
ON CONFLICT (learning_path_id, online_course_id) DO NOTHING;

INSERT INTO learning_path_courses (learning_path_id, online_course_id, display_order)
SELECT lp.id, oc.id, 3
FROM learning_paths lp, online_courses oc
WHERE lp.code = 'LP-IELTS-65' AND oc.slug = 'center-sheet-ielts-writing'
ON CONFLICT (learning_path_id, online_course_id) DO NOTHING;

INSERT INTO learning_path_courses (learning_path_id, online_course_id, display_order)
SELECT lp.id, oc.id, 4
FROM learning_paths lp, online_courses oc
WHERE lp.code = 'LP-IELTS-65' AND oc.slug = 'center-sheet-ielts-speaking'
ON CONFLICT (learning_path_id, online_course_id) DO NOTHING;

-- Attach TOEIC courses
INSERT INTO learning_path_courses (learning_path_id, online_course_id, display_order)
SELECT lp.id, oc.id, 1
FROM learning_paths lp, online_courses oc
WHERE lp.code = 'LP-TOEIC-650' AND oc.slug = 'center-sheet-toeic-lr'
ON CONFLICT (learning_path_id, online_course_id) DO NOTHING;

INSERT INTO learning_path_courses (learning_path_id, online_course_id, display_order)
SELECT lp.id, oc.id, 2
FROM learning_paths lp, online_courses oc
WHERE lp.code = 'LP-TOEIC-650' AND oc.slug = 'center-sheet-toeic-sw'
ON CONFLICT (learning_path_id, online_course_id) DO NOTHING;

-- Attach General English courses
INSERT INTO learning_path_courses (learning_path_id, online_course_id, display_order)
SELECT lp.id, oc.id, 1
FROM learning_paths lp, online_courses oc
WHERE lp.code = 'LP-COMM-WORK' AND oc.slug = 'center-sheet-grammar-foundation'
ON CONFLICT (learning_path_id, online_course_id) DO NOTHING;

INSERT INTO learning_path_courses (learning_path_id, online_course_id, display_order)
SELECT lp.id, oc.id, 2
FROM learning_paths lp, online_courses oc
WHERE lp.code = 'LP-COMM-WORK' AND oc.slug = 'center-sheet-communication-work'
ON CONFLICT (learning_path_id, online_course_id) DO NOTHING;
