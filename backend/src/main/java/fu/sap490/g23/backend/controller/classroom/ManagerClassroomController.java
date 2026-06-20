package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.*;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomTeacherRole;
import fu.sap490.g23.backend.service.classroom.ClassroomOfferingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/manager/classrooms")
@RequiredArgsConstructor
public class ManagerClassroomController {

    private final ClassroomOfferingService classroomOfferingService;

    @GetMapping
    public ResponseEntity<List<ClassroomOfferingResponse>> listOfferings() {
        return ResponseEntity.ok(classroomOfferingService.getManagerOfferings());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClassroomOfferingResponse> getOffering(@PathVariable Long id) {
        return ResponseEntity.ok(classroomOfferingService.getOffering(id, true));
    }

    @PostMapping
    public ResponseEntity<ClassroomOfferingResponse> createOffering(
            @Valid @RequestBody CreateClassroomOfferingRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.createOffering(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClassroomOfferingResponse> updateOffering(
            @PathVariable Long id,
            @Valid @RequestBody CreateClassroomOfferingRequest request
    ) {
        return ResponseEntity.ok(classroomOfferingService.updateOffering(id, request));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ClassroomOfferingResponse> publishOffering(@PathVariable Long id) {
        return ResponseEntity.ok(classroomOfferingService.publishOffering(id));
    }

    @PostMapping("/{id}/enroll")
    public ResponseEntity<ClassroomEnrollmentResponse> enrollStudent(
            @PathVariable Long id,
            @Valid @RequestBody EnrollStudentRequest request
    ) {
        return ResponseEntity.ok(classroomOfferingService.enrollStudent(id, request));
    }

    @PostMapping("/{id}/students/{studentId}/remove")
    public ResponseEntity<Void> removeStudent(@PathVariable Long id, @PathVariable Long studentId) {
        classroomOfferingService.removeStudent(id, studentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/transfer-student")
    public ResponseEntity<ClassroomEnrollmentResponse> transferStudent(
            @PathVariable Long id,
            @Valid @RequestBody TransferStudentRequest request
    ) {
        return ResponseEntity.ok(classroomOfferingService.transferStudent(id, request));
    }

    @PostMapping("/{id}/teachers/{teacherId}/assign")
    public ResponseEntity<ClassroomTeacherSummaryResponse> assignTeacher(
            @PathVariable Long id,
            @PathVariable Long teacherId,
            @RequestParam(defaultValue = "PRIMARY") ClassroomTeacherRole role
    ) {
        return ResponseEntity.ok(classroomOfferingService.assignTeacher(id, teacherId, role));
    }

    @PostMapping("/{id}/teachers/{oldTeacherId}/replace/{newTeacherId}")
    public ResponseEntity<ClassroomTeacherSummaryResponse> replaceTeacher(
            @PathVariable Long id,
            @PathVariable Long oldTeacherId,
            @PathVariable Long newTeacherId
    ) {
        return ResponseEntity.ok(classroomOfferingService.replaceTeacher(id, oldTeacherId, newTeacherId));
    }

    @PostMapping("/enrollments/{enrollmentId}/confirm")
    public ResponseEntity<ClassroomEnrollmentResponse> confirmRegistration(
            @PathVariable Long enrollmentId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.confirmRegistration(enrollmentId, authentication.getName()));
    }

    @PostMapping("/enrollments/{enrollmentId}/tuition")
    public ResponseEntity<ClassroomEnrollmentResponse> recordTuitionPayment(
            @PathVariable Long enrollmentId,
            @Valid @RequestBody RecordTuitionPaymentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.recordTuitionPayment(enrollmentId, request, authentication.getName()));
    }

    @PostMapping("/enrollments/{enrollmentId}/assign")
    public ResponseEntity<ClassroomEnrollmentResponse> assignToClass(
            @PathVariable Long enrollmentId,
            @RequestBody(required = false) AssignToClassRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(classroomOfferingService.assignToClass(enrollmentId, request, authentication.getName()));
    }

    @PostMapping("/conflict-check")
    public ResponseEntity<ConflictCheckResultResponse> checkConflict(@RequestBody ConflictCheckRequest request) {
        return ResponseEntity.ok(classroomOfferingService.checkConflict(request));
    }
}
