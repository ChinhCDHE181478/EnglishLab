package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.request.course.UpdatePackageEnrollmentRequest;
import fu.sep490.g23.backend.dto.response.course.PackageEnrollmentAdminResponse;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PackageEnrollmentAdminService {
    List<PackageEnrollmentAdminResponse> listEnrollments(EnrollmentStatus status, String keyword);
    Page<PackageEnrollmentAdminResponse> pageEnrollments(EnrollmentStatus status, String keyword, Pageable pageable);

    PackageEnrollmentAdminResponse updateEnrollment(Long enrollmentId, UpdatePackageEnrollmentRequest request, String managerEmail);
}
