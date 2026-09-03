package fu.sep490.g23.backend.controller.classroom;

import fu.sep490.g23.backend.dto.response.classroom.InstructorLedCourseResponse;
import fu.sep490.g23.backend.service.classroom.InstructorLedCourseCatalogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/course-offerings")
@RequiredArgsConstructor
public class PublicCourseOfferingController {
    private final InstructorLedCourseCatalogService instructorLedCourseCatalogService;

    @GetMapping
    public ResponseEntity<List<InstructorLedCourseResponse>> list() {
        return ResponseEntity.ok(instructorLedCourseCatalogService.listPublishedPrograms());
    }

    @GetMapping("/{idOrCode}")
    public ResponseEntity<InstructorLedCourseResponse> get(@PathVariable String idOrCode) {
        return ResponseEntity.ok(instructorLedCourseCatalogService.getPublishedProgram(idOrCode));
    }
}
