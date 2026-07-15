package fu.sap490.g23.backend.service.classroom;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;

@SpringBootTest
@Transactional
class ClassroomScheduleLockServiceIntegrationTest {

    @Autowired
    private ClassroomScheduleLockService scheduleLockService;

    @Test
    void lockDates_AcquiresPostgresTransactionLocks() {
        assertThatCode(() -> scheduleLockService.lockDates(List.of(
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 19),
                LocalDate.of(2026, 7, 20)
        ))).doesNotThrowAnyException();
    }
}
