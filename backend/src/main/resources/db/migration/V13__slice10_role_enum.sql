ALTER TABLE user_roles
    ADD COLUMN role_code varchar(40);

UPDATE user_roles user_role
SET role_code = role.code
FROM roles role
WHERE role.id = user_role.role_id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM user_roles WHERE role_code IS NULL) THEN
        RAISE EXCEPTION 'Slice 10 role migration failed: unmapped user role';
    END IF;
END $$;

ALTER TABLE user_roles
    ALTER COLUMN role_code SET NOT NULL,
    DROP COLUMN role_id CASCADE;

ALTER TABLE user_roles
    ADD CONSTRAINT uk_user_roles_user_role UNIQUE (user_id, role_code);

DROP TABLE roles;
