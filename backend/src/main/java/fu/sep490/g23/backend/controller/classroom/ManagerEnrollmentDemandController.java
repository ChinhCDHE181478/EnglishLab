package fu.sep490.g23.backend.controller.classroom;

import fu.sep490.g23.backend.dto.response.classroom.EnrollmentDemandReportResponse;
import fu.sep490.g23.backend.service.classroom.EnrollmentRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/manager/enrollment-demand")
@RequiredArgsConstructor
public class ManagerEnrollmentDemandController {
    private final EnrollmentRequestService enrollmentRequestService;

    @GetMapping
    public ResponseEntity<List<EnrollmentDemandReportResponse>> report(Authentication authentication) {
        return ResponseEntity.ok(enrollmentRequestService.getDemandReport(authentication.getName()));
    }
}
