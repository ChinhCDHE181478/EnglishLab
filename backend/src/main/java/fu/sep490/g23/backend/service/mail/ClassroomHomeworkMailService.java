package fu.sap490.g23.backend.service.mail;

import fu.sap490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sap490.g23.backend.entity.User;

public interface ClassroomHomeworkMailService {

    void sendHomeworkAssigned(User student, ClassroomHomework homework);
}
