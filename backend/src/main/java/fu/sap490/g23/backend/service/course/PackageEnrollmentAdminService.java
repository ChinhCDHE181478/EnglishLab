package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.UpdatePackageEnrollmentRequest;
import fu.sap490.g23.backend.dto.response.course.PackageEnrollmentAdminResponse;
import fu.sap490.g23.backend.entity.course.enums.EnrollmentStatus;

import java.util.List;

public interface PackageEnrollmentAdminService {
    List<PackageEnrollmentAdminResponse> listEnrollments(EnrollmentStatus status, String keyword);

    PackageEnrollmentAdminResponse updateEnrollment(Long enrollmentId, UpdatePackageEnrollmentRequest request, String managerEmail);
}
