package fu.sep490.g23.backend.service.mail;

import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.PackageEnrollment;
import fu.sep490.g23.backend.entity.User;

public interface CourseEnrollmentMailService {

    void sendEnrollmentSuccessEmail(User student, OnlineCourse course, PackageEnrollment enrollment);
}
