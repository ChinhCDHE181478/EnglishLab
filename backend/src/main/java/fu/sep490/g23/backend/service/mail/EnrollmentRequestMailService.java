package fu.sep490.g23.backend.service.mail;

import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.EnrollmentRequest;

public interface EnrollmentRequestMailService {
    void sendTestAppointment(EnrollmentRequest request);

    void sendClassAssignment(EnrollmentRequest request, ClassroomOffering classroom);
}
