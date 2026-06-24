BEGIN;

INSERT INTO package_types (code, name, description, active)
VALUES ('ONLINE_COURSE', 'Online Course', 'Self-paced online learning package', true)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    active = EXCLUDED.active;

INSERT INTO course_categories (code, name, description, display_order, active)
VALUES ('IELTS', 'IELTS', 'IELTS exam preparation courses', 1, true)
ON CONFLICT (code) DO UPDATE SET
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    display_order = EXCLUDED.display_order,
    active = EXCLUDED.active;

DELETE FROM lesson_progress
WHERE lesson_id IN (
    SELECT l.id
    FROM lessons l
    JOIN course_modules cm ON cm.id = l.module_id
    JOIN online_courses oc ON oc.id = cm.online_course_id
    JOIN packages p ON p.id = oc.package_id
    WHERE p.slug = 'e2-ielts-practice-tests'
);

DELETE FROM lessons
WHERE module_id IN (
    SELECT cm.id
    FROM course_modules cm
    JOIN online_courses oc ON oc.id = cm.online_course_id
    JOIN packages p ON p.id = oc.package_id
    WHERE p.slug = 'e2-ielts-practice-tests'
);

DELETE FROM course_modules
WHERE online_course_id IN (
    SELECT oc.id
    FROM online_courses oc
    JOIN packages p ON p.id = oc.package_id
    WHERE p.slug = 'e2-ielts-practice-tests'
);

WITH refs AS (
    SELECT
        (SELECT id FROM package_types WHERE code = 'ONLINE_COURSE') AS package_type_id,
        (SELECT id FROM course_categories WHERE code = 'IELTS') AS category_id
), upsert_package AS (
    INSERT INTO packages (
        package_type_id,
        title,
        slug,
        short_description,
        description,
        target_score,
        duration_label,
        study_mode,
        price,
        thumbnail_url,
        status,
        display_order,
        featured,
        deleted,
        created_at,
        updated_at
    )
    SELECT
        package_type_id,
        'E2 IELTS Practice Tests',
        'e2-ielts-practice-tests',
        'IELTS practice course curated from public E2 IELTS YouTube videos.',
        'An IELTS practice course for Listening, Reading, and Speaking practice. Each video is organized as one module with a study guide, the original video lesson, and follow-up practice.',
        'IELTS 5.5 - 7.0',
        '5 hours 32 minutes',
        'Self-paced online video course',
        10000,
        'https://i.ytimg.com/vi/v3axTdVoYkY/hqdefault.jpg',
        'PUBLISHED',
        5,
        true,
        false,
        NOW(),
        NOW()
    FROM refs
    ON CONFLICT (slug) DO UPDATE SET
        package_type_id = EXCLUDED.package_type_id,
        title = EXCLUDED.title,
        short_description = EXCLUDED.short_description,
        description = EXCLUDED.description,
        target_score = EXCLUDED.target_score,
        duration_label = EXCLUDED.duration_label,
        study_mode = EXCLUDED.study_mode,
        price = EXCLUDED.price,
        thumbnail_url = EXCLUDED.thumbnail_url,
        status = EXCLUDED.status,
        display_order = EXCLUDED.display_order,
        featured = EXCLUDED.featured,
        deleted = EXCLUDED.deleted,
        updated_at = NOW()
    RETURNING id
), package_ref AS (
    SELECT id FROM upsert_package
    UNION
    SELECT id FROM packages WHERE slug = 'e2-ielts-practice-tests'
), upsert_course AS (
    INSERT INTO online_courses (package_id, category_id, level, total_lessons, total_hours)
    SELECT package_ref.id, refs.category_id, 'INTERMEDIATE', 18, 6
    FROM package_ref, refs
    ON CONFLICT (package_id) DO UPDATE SET
        category_id = EXCLUDED.category_id,
        level = EXCLUDED.level,
        total_lessons = EXCLUDED.total_lessons,
        total_hours = EXCLUDED.total_hours
    RETURNING id
), course_ref AS (
    SELECT id FROM upsert_course
    UNION
    SELECT oc.id FROM online_courses oc JOIN packages p ON p.id = oc.package_id WHERE p.slug = 'e2-ielts-practice-tests'
), module_seed(display_order, title, description) AS (
    VALUES
    (1, 'Module 1: IELTS Listening Practice Test with Answers', 'Practice a full Listening test and focus on main ideas, details, and answer checking.'),
    (2, 'Module 2: IELTS Reading Practice Test with Answer Explanations', 'Reading practice with answer explanations for scanning, skimming, and locating evidence.'),
    (3, 'Module 3: Full IELTS Listening Test with Answers | 2024', 'A full Listening test to build pacing and time control under test-like conditions.'),
    (4, 'Module 4: IELTS Speaking Practice Test with Answers', 'A simulated Speaking test to improve structure, examples, and natural delivery.'),
    (5, 'Module 5: IELTS Listening: Techniques and Practice Questions', 'Learn core Listening techniques and apply them in guided practice questions.'),
    (6, 'Module 6: 100 IELTS Speaking Questions | Part 1 - 20+ IELTS Speaking Topics', 'A speaking prompt bank across common Part 1 topics to build faster response habits.')
), inserted_modules AS (
    INSERT INTO course_modules (online_course_id, title, description, display_order)
    SELECT course_ref.id, module_seed.title, module_seed.description, module_seed.display_order
    FROM course_ref, module_seed
    RETURNING id, display_order
), lesson_seed(module_order, display_order, title, description, video_url, duration_minutes, preview) AS (
    VALUES
    (1, 1, 'Lesson 1.1: Goals and strategy for Listening', 'Review the Listening test format and a quick question-reading strategy before you start.', NULL, 10, true),
    (1, 2, 'Lesson 1.2: Video practice - IELTS Listening Practice Test with Answers', 'Watch the original E2 IELTS video and track mistakes while following the guided practice flow.', 'https://www.youtube.com/watch?v=v3axTdVoYkY', 29, true),
    (1, 3, 'Lesson 1.3: Review and post-video practice', 'Log mistakes by type: keyword, synonym, number, spelling, and distractor.', NULL, 15, false),

    (2, 1, 'Lesson 2.1: Goals and strategy for Reading', 'Review how to identify keywords and predict where evidence appears in the passage.', NULL, 10, false),
    (2, 2, 'Lesson 2.2: Video practice - IELTS Reading Practice Test with Answer Explanations', 'Watch the original E2 IELTS video and track mistakes while following the guided practice flow.', 'https://www.youtube.com/watch?v=kCthrwUz68w', 26, false),
    (2, 3, 'Lesson 2.3: Review and post-video practice', 'Create a short review sheet with keywords, paraphrases, and the reason each answer is correct.', NULL, 15, false),

    (3, 1, 'Lesson 3.1: Goals and strategy for Listening', 'Prepare an answer sheet and complete the test in one pass without pausing the video.', NULL, 10, false),
    (3, 2, 'Lesson 3.2: Video practice - Full IELTS Listening Test with Answers | 2024', 'Watch the original E2 IELTS video and track mistakes while following the guided practice flow.', 'https://www.youtube.com/watch?v=VUtUOTrJ2Kk', 33, false),
    (3, 3, 'Lesson 3.3: Review and post-video practice', 'Score your work, replay difficult segments, and write short transcripts for the hardest questions.', NULL, 15, false),

    (4, 1, 'Lesson 4.1: Goals and strategy for Speaking', 'Review Fluency, Lexical Resource, Grammar Range, and Pronunciation before watching.', NULL, 10, false),
    (4, 2, 'Lesson 4.2: Video practice - IELTS Speaking Practice Test with Answers', 'Watch the original E2 IELTS video and track mistakes while following the guided practice flow.', 'https://www.youtube.com/watch?v=L520xwhFGiI', 33, false),
    (4, 3, 'Lesson 4.3: Review and post-video practice', 'Record your own answers for three prompts and self-assess with the IELTS criteria.', NULL, 15, false),

    (5, 1, 'Lesson 5.1: Goals and strategy for Listening', 'Focus on predicting, signposting, paraphrasing, and avoiding distractors.', NULL, 10, false),
    (5, 2, 'Lesson 5.2: Video practice - IELTS Listening: Techniques and Practice Questions', 'Watch the original E2 IELTS video and track mistakes while following the guided practice flow.', 'https://www.youtube.com/watch?v=6fk6W7Knld8', 36, false),
    (5, 3, 'Lesson 5.3: Review and post-video practice', 'Collect ten useful keywords or paraphrases and turn them into a personal strategy note.', NULL, 15, false),

    (6, 1, 'Lesson 6.1: Goals and strategy for Speaking', 'Choose five familiar topics and outline short answers with the Answer-Explain-Example pattern.', NULL, 10, false),
    (6, 2, 'Lesson 6.2: Video practice - 100 IELTS Speaking Questions | Part 1', 'Watch the original E2 IELTS video and track mistakes while following the guided practice flow.', 'https://www.youtube.com/watch?v=OTjzR2QCc_E', 35, false),
    (6, 3, 'Lesson 6.3: Review and post-video practice', 'Build a personal speaking bank with twenty questions, idea prompts, and strong vocabulary.', NULL, 15, false)
)
INSERT INTO lessons (module_id, title, description, content_type, content_text, video_url, material_url, duration_minutes, display_order, preview)
SELECT inserted_modules.id,
       lesson_seed.title,
       lesson_seed.description,
       CASE WHEN lesson_seed.video_url IS NULL THEN 'text' ELSE 'video' END,
       '## ' || lesson_seed.title || E'\n\n' ||
       lesson_seed.description || E'\n\n' ||
       CASE
           WHEN lesson_seed.video_url IS NULL THEN
               E'### Việc cần làm\n- Đọc mục tiêu bài học và chuẩn bị giấy ghi chú trước khi luyện tập.\n- Viết 3-5 gạch đầu dòng về chiến thuật bạn sẽ dùng trong phần thi này.\n- Sau khi hoàn tất, đánh dấu bài học là hoàn thành để tiếp tục lộ trình.'
           ELSE
               E'### Cách học với video\n- Xem video một lượt như bài thi thật, hạn chế tạm dừng khi đang làm bài.\n- Ghi lại câu sai, từ khóa bị bỏ lỡ, và dạng bẫy xuất hiện trong bài.\n- Xem lại phần khó, đối chiếu đáp án, rồi viết một ghi chú ngắn về lỗi cần tránh.\n\n### Sau khi xem\n- Đánh dấu hoàn thành bài học này để mở bước tiếp theo trong module.'
       END,
       lesson_seed.video_url,
       NULL,
       lesson_seed.duration_minutes,
       lesson_seed.display_order,
       lesson_seed.preview
FROM inserted_modules
JOIN lesson_seed ON lesson_seed.module_order = inserted_modules.display_order;

COMMIT;
