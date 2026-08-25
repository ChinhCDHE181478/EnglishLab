package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.User;

public interface VirtualAttendanceService {
    void recordVirtualJoin(ClassSchedule session, User learner);

    void finalizeVirtualAttendance(ClassSchedule session);

    void syncLarkParticipantAttendance(ClassSchedule session);
}
