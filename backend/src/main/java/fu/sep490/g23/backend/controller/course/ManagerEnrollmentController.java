package fu.sep490.g23.backend.controller.course;

import fu.sep490.g23.backend.dto.request.course.UpdatePackageEnrollmentRequest;
import fu.sep490.g23.backend.dto.response.course.PackageEnrollmentAdminResponse;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.service.course.PackageEnrollmentAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

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

    @GetMapping("/page")
    public ResponseEntity<Page<PackageEnrollmentAdminResponse>> pageEnrollments(
            @RequestParam(required = false) EnrollmentStatus status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(enrollmentAdminService.pageEnrollments(
                status,
                keyword,
                PageRequest.of(page, size, Sort.by("registeredAt").descending())
        ));
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
