package fu.sep490.g23.backend.controller.classroom;

import fu.sep490.g23.backend.dto.request.classroom.UpdateRecordingRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sep490.g23.backend.service.classroom.ClassroomRecordingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/recordings")
@RequiredArgsConstructor
public class StaffRecordingController {

    private final ClassroomRecordingService recordingService;

    @GetMapping("/classrooms/{offeringId}/sessions")
    public ResponseEntity<List<ClassroomSessionResponse>> listSessions(@PathVariable Long offeringId) {
        return ResponseEntity.ok(recordingService.listManagerSessions(offeringId));
    }

    @PutMapping("/classrooms/{offeringId}")
    public ResponseEntity<ClassroomOfferingResponse> updateOfferingRecording(
            @PathVariable Long offeringId,
            @Valid @RequestBody UpdateRecordingRequest request
    ) {
        return ResponseEntity.ok(recordingService.updateOfferingRecording(offeringId, request));
    }

    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<ClassroomSessionResponse> updateSessionRecording(
            @PathVariable Long sessionId,
            @Valid @RequestBody UpdateRecordingRequest request
    ) {
        return ResponseEntity.ok(recordingService.updateSessionRecording(sessionId, request));
    }

    @PostMapping({"/sessions/{sessionId}/sync", "/sessions/{sessionId}/sync-lark"})
    public ResponseEntity<ClassroomSessionResponse> syncRecording(@PathVariable Long sessionId) {
        return ResponseEntity.ok(recordingService.syncRecording(sessionId));
    }
}
