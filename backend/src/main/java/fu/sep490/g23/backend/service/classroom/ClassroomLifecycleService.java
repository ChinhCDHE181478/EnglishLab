package fu.sep490.g23.backend.service.classroom;

import java.time.LocalDateTime;

public interface ClassroomLifecycleService {

    void reconcileStatuses(LocalDateTime now);
}
