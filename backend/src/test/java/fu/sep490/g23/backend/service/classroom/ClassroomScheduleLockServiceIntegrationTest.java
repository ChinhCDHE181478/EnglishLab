package fu.sep490.g23.backend.service.classroom;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@Transactional
class ClassroomScheduleLockServiceIntegrationTest {

    @Autowired
    private ClassroomScheduleLockService scheduleLockService;

    @Autowired
    private Environment environment;

    @BeforeEach
    void requirePostgreSqlAdvisoryLocks() {
        String url = environment.getProperty("spring.datasource.url", "");
        Assumptions.assumeTrue(
                url.contains("postgresql"),
                "pg_advisory_xact_lock requires PostgreSQL (skipped on H2 test profile)"
        );
    }

    @Test
    void lockDates_AcquiresPostgresTransactionLocks() {
        assertThatCode(() -> scheduleLockService.lockDates(List.of(
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 19),
                LocalDate.of(2026, 7, 20)
        ))).doesNotThrowAnyException();
    }
}
