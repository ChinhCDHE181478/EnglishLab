package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.service.classroom.ClassroomScheduleLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;

@Service
@RequiredArgsConstructor
public class ClassroomScheduleLockServiceImpl implements ClassroomScheduleLockService {

    private static final int CLASSROOM_SCHEDULE_LOCK_NAMESPACE = 49023;

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockDate(LocalDate date) {
        if (date == null) {
            return;
        }

        int epochDay = Math.toIntExact(date.toEpochDay());
        jdbcTemplate.query(
                "SELECT pg_advisory_xact_lock(?, ?)",
                preparedStatement -> {
                    preparedStatement.setInt(1, CLASSROOM_SCHEDULE_LOCK_NAMESPACE);
                    preparedStatement.setInt(2, epochDay);
                },
                resultSet -> {
                    // The PostgreSQL function returns only after this transaction owns the lock.
                }
        );
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void lockDates(Collection<LocalDate> dates) {
        if (dates == null) {
            return;
        }

        dates.stream()
                .filter(java.util.Objects::nonNull)
                .distinct()
                .sorted()
                .forEach(this::lockDate);
    }
}
