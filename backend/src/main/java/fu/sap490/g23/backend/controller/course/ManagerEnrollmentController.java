package fu.sap490.g23.backend.controller.course;

import fu.sap490.g23.backend.dto.request.course.UpdatePackageEnrollmentRequest;
import fu.sap490.g23.backend.dto.response.course.PackageEnrollmentAdminResponse;
import fu.sap490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sap490.g23.backend.service.course.PackageEnrollmentAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/enrollments")
@RequiredArgsConstructor
public class ManagerEnrollmentController {

    private final PackageEnrollmentAdminService enrollmentAdminService;

    @GetMapping
    public ResponseEntity<List<PackageEnrollmentAdminResponse>> listEnrollments(
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(enrollmentAdminService.listEnrollments(status, keyword));
    }

    @PutMapping("/{enrollmentId}")
    public ResponseEntity<PackageEnrollmentAdminResponse> updateEnrollment(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody UpdatePackageEnrollmentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(enrollmentAdminService.updateEnrollment(enrollmentId, request, authentication.getName()));
    }
}
