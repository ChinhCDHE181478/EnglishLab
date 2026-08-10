package fu.sap490.g23.backend.migration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CentralCampusDataRepairMigration implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        jdbcTemplate.update("""
                update classroom_campuses
                set name = 'EnglishLab Center',
                    note = 'Cơ sở trung tâm của EnglishLab.',
                    active = true
                where id = 1
                  and (name like '%%?%%' or note like '%%?%%')
                """);
    }
}
