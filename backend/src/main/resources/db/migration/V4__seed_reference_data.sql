INSERT INTO roles (code, name, description, active)
VALUES
    ('LEARNER', 'Học viên', 'Người học sử dụng khóa học và lớp học.', TRUE),
    ('TEACHER', 'Giáo viên', 'Giáo viên phụ trách giảng dạy và chấm bài.', TRUE),
    ('MANAGER', 'Quản lý', 'Người phê duyệt và theo dõi vận hành trung tâm.', TRUE),
    ('CONTENT_MANAGER', 'Quản lý nội dung', 'Người biên soạn và quản lý học liệu.', TRUE),
    ('STAFF', 'Nhân viên đào tạo', 'Nhân viên vận hành lớp học và ghi danh.', TRUE),
    ('ADMIN', 'Quản trị viên', 'Người quản trị hệ thống.', TRUE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    active = TRUE;

INSERT INTO package_types (code, name, description, active)
VALUES
    ('ONLINE_COURSE', 'Khóa học Online', 'Khóa học tự học trực tuyến.', TRUE),
    ('CLASSROOM', 'Lớp học', 'Lớp học có giáo viên phụ trách.', TRUE),
    ('BUNDLE', 'Gói học', 'Nhóm quyền truy cập nhiều sản phẩm học tập.', TRUE),
    ('MOCK_TEST', 'Đề thi thử', 'Sản phẩm thi thử độc lập.', TRUE),
    ('SUBSCRIPTION', 'Gói thuê bao', 'Quyền truy cập theo chu kỳ.', TRUE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    active = TRUE;

INSERT INTO course_categories (code, name, description, display_order, active)
VALUES
    ('IELTS', 'IELTS', 'Khóa học luyện thi IELTS.', 1, TRUE),
    ('TOEIC', 'TOEIC', 'Khóa học luyện thi TOEIC.', 2, TRUE),
    ('COMMUNICATION', 'Giao tiếp', 'Khóa học tiếng Anh giao tiếp.', 3, TRUE),
    ('FOUNDATION', 'Nền tảng', 'Khóa học củng cố nền tảng tiếng Anh.', 4, TRUE),
    ('ONLINE', 'Trực tuyến', 'Chương trình học trực tuyến linh hoạt.', 5, TRUE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    display_order = EXCLUDED.display_order,
    active = TRUE;
