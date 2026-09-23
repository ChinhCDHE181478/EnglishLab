package fu.sep490.g23.backend.service.mail;

import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;

import java.time.LocalDateTime;

public interface EnrollmentRequestMailService {
    void sendTestAppointment(
            CourseRegistrationRequest request,
            LocalDateTime appointmentAt,
            String location
    );

    void sendTestResult(CourseRegistrationRequest request, boolean eligible, String evaluatedCourseTitle);

    void sendClassAssignment(
            CourseRegistrationRequest request,
            ClassSection classroom,
            ClassroomEnrollmentResponse enrollment
    );
}
