package fu.sep490.g23.backend.service.mail;

import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest;

public interface EnrollmentRequestMailService {
    void sendTestAppointment(CourseRegistrationRequest request);

    void sendTestResult(CourseRegistrationRequest request, boolean eligible, String evaluatedCourseTitle);

    void sendClassAssignment(CourseRegistrationRequest request, ClassSection classroom);
}
