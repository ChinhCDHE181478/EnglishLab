package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CreateAttendanceDisputeRequest;
import fu.sap490.g23.backend.dto.request.classroom.ReviewAttendanceDisputeRequest;
import fu.sap490.g23.backend.dto.response.classroom.AttendanceDisputeResponse;
import fu.sap490.g23.backend.service.classroom.ClassroomAttendanceDisputeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ClassroomAttendanceDisputeController {

    private final ClassroomAttendanceDisputeService disputeService;

    @PostMapping("/api/student/attendance/{attendanceId}/disputes")
    public ResponseEntity<AttendanceDisputeResponse> createDispute(
            @PathVariable Long attendanceId,
            @Valid @RequestBody CreateAttendanceDisputeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(disputeService.create(attendanceId, request, authentication.getName()));
    }

    @GetMapping("/api/student/attendance/disputes")
    public ResponseEntity<List<AttendanceDisputeResponse>> listMyDisputes(Authentication authentication) {
        return ResponseEntity.ok(disputeService.listForStudent(authentication.getName()));
    }

    @GetMapping("/api/teacher/classrooms/{offeringId}/attendance-disputes")
    public ResponseEntity<List<AttendanceDisputeResponse>> listForClass(@PathVariable Long offeringId) {
        return ResponseEntity.ok(disputeService.listForClass(offeringId));
    }

    @GetMapping("/api/training-manager/attendance-disputes/pending")
    public ResponseEntity<List<AttendanceDisputeResponse>> listPending() {
        return ResponseEntity.ok(disputeService.listPending());
    }

    @PostMapping("/api/training-manager/attendance-disputes/{disputeId}/review")
    public ResponseEntity<AttendanceDisputeResponse> review(
            @PathVariable Long disputeId,
            @Valid @RequestBody ReviewAttendanceDisputeRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(disputeService.review(disputeId, request, authentication.getName()));
    }
}
