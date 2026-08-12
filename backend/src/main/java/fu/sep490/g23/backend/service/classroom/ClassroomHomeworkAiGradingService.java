package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;

public interface ClassroomHomeworkAiGradingService {

    boolean tryAutoGrade(ClassroomHomeworkSubmission submission);
}
