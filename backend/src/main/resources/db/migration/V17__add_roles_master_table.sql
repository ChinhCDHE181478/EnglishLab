CREATE TABLE roles (
    code varchar(40) PRIMARY KEY,
    display_name varchar(100) NOT NULL,
    description varchar(500),
    active boolean NOT NULL DEFAULT true,
    created_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamp without time zone NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_roles_code_format CHECK (code = upper(code) AND code ~ '^[A-Z][A-Z0-9_]*$')
);

INSERT INTO roles (code, display_name, description) VALUES
    ('LEARNER', 'Học viên', 'Người học sử dụng khóa học và lớp học.'),
    ('TEACHER', 'Giáo viên', 'Giáo viên giảng dạy và chấm bài.'),
    ('MANAGER', 'Quản lý', 'Quản lý vận hành và phê duyệt.'),
    ('CONTENT_MANAGER', 'Quản lý nội dung', 'Quản lý khóa học và học liệu.'),
    ('STAFF', 'Nhân viên đào tạo', 'Nhân viên hỗ trợ vận hành đào tạo.'),
    ('ADMIN', 'Quản trị viên', 'Quản trị người dùng và hệ thống.');

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM user_roles user_role
        LEFT JOIN roles role ON role.code = user_role.role_code
        WHERE role.code IS NULL
    ) THEN
        RAISE EXCEPTION 'Cannot add roles FK: user_roles contains unknown role_code values';
    END IF;
END $$;

ALTER TABLE user_roles
    DROP CONSTRAINT IF EXISTS uk_user_roles_user_role,
    ADD CONSTRAINT pk_user_roles PRIMARY KEY (user_id, role_code),
    ADD CONSTRAINT fk_user_roles_role_code
        FOREIGN KEY (role_code) REFERENCES roles(code) ON UPDATE CASCADE ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_user_roles_role_code ON user_roles(role_code);
