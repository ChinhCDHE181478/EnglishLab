package fu.sap490.g23.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class LegacyUserRoleMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.execute("alter table users drop constraint if exists users_role_check");
        jdbcTemplate.execute("""
                alter table users
                add constraint users_role_check
                check (role in ('USER', 'LEARNER', 'TEACHER', 'MANAGER', 'CONTENT_MANAGER', 'TEACHER_MANAGER', 'ADMIN'))
                """);

        int updatedRows = jdbcTemplate.update(
                "update users set role = ? where upper(role) = ?",
                "LEARNER",
                "USER"
        );

        jdbcTemplate.execute("alter table users drop constraint if exists users_role_check");
        jdbcTemplate.execute("""
                alter table users
                add constraint users_role_check
                check (role in ('LEARNER', 'TEACHER', 'MANAGER', 'CONTENT_MANAGER', 'TEACHER_MANAGER', 'ADMIN'))
                """);

        if (updatedRows > 0) {
            log.info("Migrated {} legacy USER role records to LEARNER.", updatedRows);
        }
    }
}
