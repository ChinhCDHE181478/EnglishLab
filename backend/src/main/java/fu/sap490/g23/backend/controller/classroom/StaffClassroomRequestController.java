package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.ReviewChangeRequestRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomChangeRequestResponse;
import fu.sap490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sap490.g23.backend.service.classroom.ClassroomChangeRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/requests")
@RequiredArgsConstructor
public class StaffClassroomRequestController {

    private final ClassroomChangeRequestService changeRequestService;

    @GetMapping("/pending")
    public ResponseEntity<List<ClassroomChangeRequestResponse>> listPending() {
        return ResponseEntity.ok(changeRequestService.listPending());
    }

    @PostMapping("/{requestId}/approve")
    public ResponseEntity<ClassroomChangeRequestResponse> approve(
            @PathVariable Long requestId,
            @Valid @RequestBody ReviewChangeRequestRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(changeRequestService.approve(requestId, request, authentication.getName()));
    }

    @PostMapping("/{requestId}/reject")
    public ResponseEntity<ClassroomChangeRequestResponse> reject(
            @PathVariable Long requestId,
            @Valid @RequestBody ReviewChangeRequestRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(changeRequestService.reject(requestId, request, authentication.getName()));
    }

    @PostMapping("/{requestId}/conflict-check")
    public ResponseEntity<ConflictCheckResultResponse> checkConflict(@PathVariable Long requestId) {
        return ResponseEntity.ok(changeRequestService.checkPendingConflict(requestId));
    }
}
