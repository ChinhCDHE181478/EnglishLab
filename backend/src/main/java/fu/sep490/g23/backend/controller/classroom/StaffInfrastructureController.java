package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.*;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.service.classroom.ClassroomInfrastructureService;
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

}
