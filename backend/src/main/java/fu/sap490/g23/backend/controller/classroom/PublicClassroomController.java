package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sap490.g23.backend.entity.classroom.ClassroomDeliveryMode;
import fu.sap490.g23.backend.service.classroom.ClassroomOfferingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classroom-offerings")
@RequiredArgsConstructor
public class PublicClassroomController {

    private final ClassroomOfferingService classroomOfferingService;

    @GetMapping
    public ResponseEntity<Page<ClassroomOfferingResponse>> getOfferings(
            @RequestParam(required = false) ClassroomDeliveryMode mode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(classroomOfferingService.getPublicOfferings(mode, pageable));
    }

    @GetMapping("/{slugOrId}")
    public ResponseEntity<ClassroomOfferingResponse> getOffering(@PathVariable String slugOrId) {
        return ResponseEntity.ok(classroomOfferingService.getPublicOffering(slugOrId));
    }
}
