package fu.sep490.g23.backend.service.mail;

import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.User;

public interface ClassroomHomeworkMailService {

    void sendHomeworkAssigned(User student, ClassroomHomework homework);
}
