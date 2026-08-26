package fu.sep490.g23.backend.controller.classroom;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomRoomDetailResponse;
import fu.sep490.g23.backend.dto.request.classroom.UpsertRoomRequest;

import fu.sep490.g23.backend.service.classroom.ClassroomInfrastructureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/staff/infrastructure")
@RequiredArgsConstructor
public class StaffInfrastructureController {

    private final ClassroomInfrastructureService infrastructureService;

    @GetMapping("/rooms")
    public ResponseEntity<List<ClassroomRoomDetailResponse>> listRooms() {
        return ResponseEntity.ok(infrastructureService.listRooms());
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

}
