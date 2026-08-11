package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomSession;
import fu.sep490.g23.backend.entity.User;

public interface VirtualAttendanceService {
    void recordVirtualJoin(ClassroomSession session, User learner);

    void finalizeVirtualAttendance(ClassroomSession session);

    void syncLarkParticipantAttendance(ClassroomSession session);
}
