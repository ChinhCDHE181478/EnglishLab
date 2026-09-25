-- Keep score terminology and data aligned with each course category.
UPDATE online_courses course
SET recommended_current_band_min = NULL,
    target_band = NULL
FROM course_categories category
WHERE course.category_id = category.id
  AND category.code <> 'IELTS';

UPDATE online_courses course
SET target_score = NULLIF(regexp_replace(COALESCE(course.target_score, ''), '[^0-9]', '', 'g'), '')
FROM course_categories category
WHERE course.category_id = category.id
  AND category.code = 'TOEIC';

UPDATE online_courses course
SET target_score = CASE category.code
        WHEN 'COMMUNICATION' THEN COALESCE(NULLIF(BTRIM(course.target_score), ''), 'CEFR B1-B2')
        WHEN 'BUSINESS' THEN COALESCE(NULLIF(BTRIM(course.target_score), ''), 'CEFR B1-B2')
        WHEN 'FOUNDATION' THEN COALESCE(NULLIF(BTRIM(course.target_score), ''), 'CEFR A2-B1')
        ELSE course.target_score
    END
FROM course_categories category
WHERE course.category_id = category.id
  AND category.code IN ('COMMUNICATION', 'BUSINESS', 'FOUNDATION');

-- Complete published instructor-led course profiles already present in production.
UPDATE instructor_led_courses
SET short_description = COALESCE(NULLIF(BTRIM(short_description), ''),
        'Xây dựng nền tảng IELTS bốn kỹ năng với giảng viên hướng dẫn.'),
    description = COALESCE(NULLIF(BTRIM(description), ''),
        'Chương trình IELTS nền tảng có giảng viên hướng dẫn, luyện đồng đều Listening, Reading, Writing và Speaking.'),
    entry_level = COALESCE(NULLIF(BTRIM(entry_level), ''), 'IELTS 3.5 hoặc tương đương'),
    focus_skills = COALESCE(NULLIF(BTRIM(focus_skills), ''), 'LISTENING,READING,WRITING,SPEAKING'),
    target_band = 5.5,
    target_score = NULL,
    duration_label = COALESCE(NULLIF(BTRIM(duration_label), ''), '12 tuần'),
    learning_outcomes = COALESCE(NULLIF(BTRIM(learning_outcomes), ''),
        'Củng cố nền tảng bốn kỹ năng và sẵn sàng cho lộ trình IELTS 6.5.'),
    teacher_guide = COALESCE(NULLIF(BTRIM(teacher_guide), ''),
        'Bám sát mục tiêu từng Unit, giao bài sau mỗi buổi và phản hồi theo tiêu chí IELTS.')
WHERE code = 'ILC_IELTS_FOUNDATION';

UPDATE instructor_led_courses
SET short_description = COALESCE(NULLIF(BTRIM(short_description), ''),
        'Luyện thi IELTS tăng tốc hướng tới Band 6.5.'),
    description = COALESCE(NULLIF(BTRIM(description), ''),
        'Chương trình IELTS tăng tốc có giảng viên, tập trung chiến thuật và thực hành bốn kỹ năng.'),
    entry_level = COALESCE(NULLIF(BTRIM(entry_level), ''), 'IELTS 5.0 hoặc tương đương'),
    focus_skills = COALESCE(NULLIF(BTRIM(focus_skills), ''), 'LISTENING,READING,WRITING,SPEAKING'),
    target_band = 6.5,
    target_score = NULL,
    duration_label = COALESCE(NULLIF(BTRIM(duration_label), ''), '10 tuần'),
    learning_outcomes = COALESCE(NULLIF(BTRIM(learning_outcomes), ''),
        'Hoàn thiện chiến thuật bốn kỹ năng và hướng tới IELTS 6.5.'),
    teacher_guide = COALESCE(NULLIF(BTRIM(teacher_guide), ''),
        'Theo dõi tiến độ từng kỹ năng, chữa bài Writing/Speaking và tổ chức kiểm tra định kỳ.')
WHERE code = 'ILC_IELTS_65';

UPDATE instructor_led_courses
SET short_description = COALESCE(NULLIF(BTRIM(short_description), ''),
        'Luyện IELTS Speaking theo Part với phản hồi trực tiếp từ giảng viên.'),
    entry_level = COALESCE(NULLIF(BTRIM(entry_level), ''), 'IELTS Speaking 5.0 hoặc tương đương'),
    focus_skills = 'SPEAKING',
    target_band = 6.5,
    target_score = NULL,
    description = COALESCE(NULLIF(BTRIM(description), ''),
        'Chương trình luyện IELTS Speaking có giảng viên, tập trung Fluency, Pronunciation và phát triển ý Part 1-3.'),
    duration_label = COALESCE(NULLIF(BTRIM(duration_label), ''), '12 tuần'),
    learning_outcomes = COALESCE(NULLIF(BTRIM(learning_outcomes), ''),
        'Trình bày câu trả lời mạch lạc, phát âm rõ và hướng tới IELTS Speaking 6.5.'),
    teacher_guide = COALESCE(NULLIF(BTRIM(teacher_guide), ''),
        'Tổ chức speaking drill, cue card và phản hồi theo bốn tiêu chí IELTS Speaking.')
WHERE code = 'ILC_IELTS_SPEAKING';

UPDATE instructor_led_courses
SET short_description = COALESCE(NULLIF(BTRIM(short_description), ''),
        'Luyện TOEIC Listening và Reading hướng tới 650 điểm.'),
    description = COALESCE(NULLIF(BTRIM(description), ''),
        'Chương trình TOEIC Listening & Reading có giảng viên, bao quát Part 1-7 và chiến thuật làm bài.'),
    entry_level = COALESCE(NULLIF(BTRIM(entry_level), ''), 'TOEIC 450 hoặc tương đương'),
    focus_skills = COALESCE(NULLIF(BTRIM(focus_skills), ''), 'LISTENING,READING'),
    target_band = NULL,
    target_score = 650,
    duration_label = COALESCE(NULLIF(BTRIM(duration_label), ''), '10 tuần'),
    learning_outcomes = COALESCE(NULLIF(BTRIM(learning_outcomes), ''),
        'Nắm cấu trúc Part 1-7, cải thiện tốc độ và hướng tới TOEIC 650.'),
    teacher_guide = COALESCE(NULLIF(BTRIM(teacher_guide), ''),
        'Luyện theo từng Part, duy trì error log và kiểm tra tiến độ Listening/Reading hằng tuần.')
WHERE code = 'ILC_TOEIC_650';

UPDATE instructor_led_courses
SET short_description = COALESCE(NULLIF(BTRIM(short_description), ''),
        'Luyện TOEIC tăng tốc từ nền tảng 650 hướng tới 800 điểm.'),
    target_score = 800,
    target_band = NULL,
    entry_level = 'TOEIC 650 hoặc tương đương',
    focus_skills = COALESCE(NULLIF(BTRIM(focus_skills), ''), 'LISTENING,READING'),
    description = COALESCE(NULLIF(BTRIM(description), ''),
        'Chương trình TOEIC tăng tốc có giảng viên cho học viên đã đạt khoảng 650 và hướng tới 800.'),
    duration_label = COALESCE(NULLIF(BTRIM(duration_label), ''), '12 tuần'),
    learning_outcomes = COALESCE(NULLIF(BTRIM(learning_outcomes), ''),
        'Tăng tốc độ xử lý Part 5-7, kiểm soát bẫy Listening và hướng tới TOEIC 800.'),
    teacher_guide = COALESCE(NULLIF(BTRIM(teacher_guide), ''),
        'Phân tích lỗi theo Part, giao timed practice và tổ chức mock test định kỳ.')
WHERE code = 'ILC_TOEIC_800';

UPDATE instructor_led_courses
SET short_description = COALESCE(NULLIF(BTRIM(short_description), ''),
        'Phát triển tiếng Anh giao tiếp thực tế trong môi trường công sở.'),
    description = COALESCE(NULLIF(BTRIM(description), ''),
        'Chương trình tiếng Anh công sở có giảng viên, luyện giao tiếp trong họp, email, điện thoại và thuyết trình.'),
    entry_level = COALESCE(NULLIF(BTRIM(entry_level), ''), 'CEFR A2 hoặc tương đương'),
    focus_skills = COALESCE(NULLIF(BTRIM(focus_skills), ''), 'SPEAKING,LISTENING,WRITING'),
    target_band = NULL,
    target_score = NULL,
    duration_label = COALESCE(NULLIF(BTRIM(duration_label), ''), '24 buổi'),
    learning_outcomes = COALESCE(NULLIF(BTRIM(learning_outcomes), ''),
        'Giao tiếp công sở ở mức CEFR B1, viết email rõ ràng và trình bày ngắn có cấu trúc.'),
    teacher_guide = COALESCE(NULLIF(BTRIM(teacher_guide), ''),
        'Ưu tiên role-play, tình huống công sở và phản hồi thực hành sau mỗi buổi.')
WHERE code = 'ILC_COMM_WORK';

UPDATE instructor_led_courses
SET teacher_guide = COALESCE(NULLIF(BTRIM(teacher_guide), ''),
        'Bám sát mục tiêu từng Unit, giao bài sau mỗi buổi và phản hồi theo tiêu chí của kỳ thi.')
WHERE publication_status = 'PUBLISHED'
  AND (teacher_guide IS NULL OR BTRIM(teacher_guide) = '');
