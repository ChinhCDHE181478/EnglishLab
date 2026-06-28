package fu.sap490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class LegacyUserRoleMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        migrateLegacyRoleColumn();
    }

    private void seedRoles() {
        jdbcTemplate.update("""
                insert into roles (code, name, description, active)
                values
                    ('LEARNER', 'Học viên', 'Học và thực hiện các bài đánh giá.', true),
                    ('TEACHER', 'Giáo viên', 'Giảng dạy và quản lý hoạt động lớp được phân công.', true),
                    ('MANAGER', 'Quản lý', 'Quản lý vận hành chung của hệ thống.', true),
                    ('CONTENT_MANAGER', 'Quản lý nội dung', 'Quản lý khóa học và nội dung học tập.', true),
                    ('TRAINING_MANAGER', 'Quản lý đào tạo', 'Quản lý đăng ký, lớp học và hoạt động đào tạo.', true),
                    ('ADMIN', 'Quản trị viên', 'Có quyền quản trị cao nhất trong hệ thống.', true)
                on conflict (code) do update
                set name = excluded.name,
                    description = excluded.description,
                    active = true
                """);
    }

    private void migrateLegacyRoleColumn() {
        Boolean legacyColumnExists = jdbcTemplate.queryForObject("""
                select exists (
                    select 1
                    from information_schema.columns
                    where table_schema = current_schema()
                      and table_name = 'users'
                      and column_name = 'role'
                )
                """, Boolean.class);

        if (!Boolean.TRUE.equals(legacyColumnExists)) {
            return;
        }

        jdbcTemplate.execute("alter table users drop constraint if exists users_role_check");
        jdbcTemplate.update("update users set role = 'LEARNER' where upper(role) = 'USER'");
        jdbcTemplate.update("update users set role = 'TRAINING_MANAGER' where upper(role) = 'TEACHER_MANAGER'");

        int migratedRows = jdbcTemplate.update("""
                insert into user_roles (user_id, role_id)
                select users.id, roles.id
                from users
                join roles on roles.code = upper(users.role)
                where users.role is not null
                on conflict (user_id, role_id) do nothing
                """);

        jdbcTemplate.execute("alter table users drop column role");
        log.info("Migrated {} user-role assignments to normalized role tables.", migratedRows);
    }
}
