package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.User;

public interface VirtualAttendanceService {
    void recordVirtualJoin(ClassroomSession session, User learner);

    void finalizeVirtualAttendance(ClassroomSession session);

    void syncLarkParticipantAttendance(ClassroomSession session);
}
