package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.ConfirmEnrollmentPlacementRequest;
import fu.sap490.g23.backend.dto.request.classroom.RejectEnrollmentRequest;
import fu.sap490.g23.backend.dto.request.classroom.CompleteEnrollmentConsultationRequest;
import fu.sap490.g23.backend.dto.request.classroom.AssignEnrollmentClassRequest;
import fu.sap490.g23.backend.dto.response.classroom.CourseEnrollmentRequestResponse;
import fu.sap490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sap490.g23.backend.service.classroom.EnrollmentRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/staff/enrollment-requests")
@RequiredArgsConstructor
public class StaffEnrollmentRequestController {
    private final EnrollmentRequestService enrollmentRequestService;

    @GetMapping
    public ResponseEntity<List<CourseEnrollmentRequestResponse>> list(
            @RequestParam(required = false) EnrollmentRequestStatus status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(enrollmentRequestService.listForStaff(status, authentication.getName()));
    }

    @PatchMapping("/{requestId}/placement-level")
    public ResponseEntity<CourseEnrollmentRequestResponse> confirmPlacementLevel(
            @PathVariable Long requestId,
            @Valid @RequestBody ConfirmEnrollmentPlacementRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(enrollmentRequestService.confirmPlacementLevel(
                requestId,
                request,
                authentication.getName()
        ));
    }

    @PatchMapping("/{requestId}/reject")
    public ResponseEntity<CourseEnrollmentRequestResponse> reject(
            @PathVariable Long requestId,
            @Valid @RequestBody RejectEnrollmentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(enrollmentRequestService.reject(
                requestId,
                request,
                authentication.getName()
        ));
    }

    @PatchMapping("/{requestId}/consultation-complete")
    public ResponseEntity<CourseEnrollmentRequestResponse> completeConsultation(
            @PathVariable Long requestId,
            @Valid @RequestBody CompleteEnrollmentConsultationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(enrollmentRequestService.completeConsultation(
                requestId,
                request,
                authentication.getName()
        ));
    }

    @PatchMapping("/{requestId}/assign-class")
    public ResponseEntity<CourseEnrollmentRequestResponse> assignClass(
            @PathVariable Long requestId,
            @Valid @RequestBody AssignEnrollmentClassRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(enrollmentRequestService.assignClass(
                requestId,
                request,
                authentication.getName()
        ));
    }
}
