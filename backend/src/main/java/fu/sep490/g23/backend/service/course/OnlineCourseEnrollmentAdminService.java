package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.request.course.UpdateOnlineCourseEnrollmentRequest;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseEnrollmentAdminResponse;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OnlineCourseEnrollmentAdminService {
    List<OnlineCourseEnrollmentAdminResponse> listEnrollments(EnrollmentStatus status, String keyword);
    Page<OnlineCourseEnrollmentAdminResponse> pageEnrollments(EnrollmentStatus status, String keyword, Pageable pageable);

    OnlineCourseEnrollmentAdminResponse updateEnrollment(Long enrollmentId, UpdateOnlineCourseEnrollmentRequest request, String managerEmail);
}
