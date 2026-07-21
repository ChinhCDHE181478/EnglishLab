package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.*;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.service.classroom.ClassroomInfrastructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/staff/infrastructure", "/api/training-manager/infrastructure"})
@RequiredArgsConstructor
public class TrainingManagerInfrastructureController {

    private final ClassroomInfrastructureService infrastructureService;

    @GetMapping("/campuses")
    public ResponseEntity<List<ClassroomCampusResponse>> listCampuses() {
        return ResponseEntity.ok(infrastructureService.listCampuses());
    }

    @PostMapping("/campuses")
    public ResponseEntity<ClassroomCampusResponse> createCampus(@Valid @RequestBody UpsertCampusRequest request) {
        return ResponseEntity.ok(infrastructureService.createCampus(request));
    }

    @PutMapping("/campuses/{id}")
    public ResponseEntity<ClassroomCampusResponse> updateCampus(
            @PathVariable Long id,
            @Valid @RequestBody UpsertCampusRequest request
    ) {
        return ResponseEntity.ok(infrastructureService.updateCampus(id, request));
    }

    @GetMapping("/rooms")
    public ResponseEntity<List<ClassroomRoomDetailResponse>> listRooms(@RequestParam(required = false) Long campusId) {
        return ResponseEntity.ok(infrastructureService.listRooms(campusId));
    }

    @PostMapping("/rooms")
    public ResponseEntity<ClassroomRoomDetailResponse> createRoom(@Valid @RequestBody UpsertRoomRequest request) {
        return ResponseEntity.ok(infrastructureService.createRoom(request));
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<ClassroomRoomDetailResponse> updateRoom(
            @PathVariable Long id,
            @Valid @RequestBody UpsertRoomRequest request
    ) {
        return ResponseEntity.ok(infrastructureService.updateRoom(id, request));
    }

    @GetMapping("/session-templates")
    public ResponseEntity<List<ClassroomSessionTemplateResponse>> listTemplates() {
        return ResponseEntity.ok(infrastructureService.listSessionTemplates());
    }

    @PostMapping("/session-templates")
    public ResponseEntity<ClassroomSessionTemplateResponse> createTemplate(
            @Valid @RequestBody UpsertSessionTemplateRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(infrastructureService.createSessionTemplate(request, authentication.getName()));
    }

    @PutMapping("/session-templates/{id}")
    public ResponseEntity<ClassroomSessionTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody UpsertSessionTemplateRequest request
    ) {
        return ResponseEntity.ok(infrastructureService.updateSessionTemplate(id, request));
    }

    @PostMapping("/classrooms/{offeringId}/generate-sessions")
    public ResponseEntity<List<ClassroomSessionResponse>> generateSessions(
            @PathVariable Long offeringId,
            @Valid @RequestBody GenerateSessionsFromTemplateRequest request
    ) {
        return ResponseEntity.ok(infrastructureService.generateSessionsFromTemplate(offeringId, request));
    }
}
