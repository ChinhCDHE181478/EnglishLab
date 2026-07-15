package fu.sap490.g23.backend.service.classroom;

import java.time.LocalDate;
import java.util.Collection;

/**
 * Serializes schedule mutations that affect the same calendar date.
 */
public interface ClassroomScheduleLockService {

    void lockDate(LocalDate date);

    void lockDates(Collection<LocalDate> dates);
}
