package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.*;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.entity.classroom.ClassroomRegistrationStatus;
import fu.sap490.g23.backend.service.classroom.ClassroomOfferingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/training-manager/classrooms")
@RequiredArgsConstructor
public class TrainingManagerClassroomController {

    private final ClassroomOfferingService classroomOfferingService;

    @GetMapping
    public ResponseEntity<List<ClassroomOfferingResponse>> listOfferings() {
        return ResponseEntity.ok(classroomOfferingService.getManagerOfferings());
    }

    @GetMapping("/registrations")
    public ResponseEntity<List<ClassroomEnrollmentResponse>> listRegistrations(
            @RequestParam(required = false) ClassroomRegistrationStatus status
    ) {
        return ResponseEntity.ok(classroomOfferingService.listRegistrations(status));
    }

    @GetMapping("/enrollments/{enrollmentId}")
    public ResponseEntity<ClassroomEnrollmentResponse> getEnrollment(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(classroomOfferingService.getRegistration(enrollmentId));
    }

    @PostMapping("/enrollments/{enrollmentId}/confirm")
    public ResponseEntity<ClassroomEnrollmentResponse> confirmRegistration(
            @PathVariable Long enrollmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.confirmRegistration(enrollmentId, authentication.getName()));
    }

    @PostMapping("/enrollments/{enrollmentId}/reject")
    public ResponseEntity<ClassroomEnrollmentResponse> rejectRegistration(
            @PathVariable Long enrollmentId,
            @RequestBody(required = false) RejectRegistrationRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.rejectRegistration(enrollmentId, request, authentication.getName()));
    }

    @PostMapping("/enrollments/{enrollmentId}/tuition")
    public ResponseEntity<ClassroomEnrollmentResponse> recordTuitionPayment(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody RecordTuitionPaymentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.recordTuitionPayment(enrollmentId, request, authentication.getName()));
    }

    @GetMapping("/enrollments/{enrollmentId}/tuition-history")
    public ResponseEntity<List<ClassroomTuitionPaymentResponse>> getTuitionHistory(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(classroomOfferingService.getTuitionHistory(enrollmentId));
    }

    @PostMapping("/enrollments/{enrollmentId}/assign")
    public ResponseEntity<ClassroomEnrollmentResponse> assignToClass(
            @PathVariable Long enrollmentId,
            @RequestBody(required = false) AssignToClassRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.assignToClass(enrollmentId, request, authentication.getName()));
    }

    @PostMapping("/enrollments/{enrollmentId}/transfer")
    public ResponseEntity<ClassroomEnrollmentResponse> transferEnrollment(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody TransferEnrollmentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.transferEnrollment(enrollmentId, request, authentication.getName()));
    }

    @PostMapping("/enrollments/{enrollmentId}/conflict-check")
    public ResponseEntity<ConflictCheckResultResponse> checkEnrollmentConflict(@PathVariable Long enrollmentId) {
        return ResponseEntity.ok(classroomOfferingService.checkEnrollmentConflict(enrollmentId));
    }
}
