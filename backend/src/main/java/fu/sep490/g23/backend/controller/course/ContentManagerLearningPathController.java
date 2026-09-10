package fu.sep490.g23.backend.controller.course;

import fu.sep490.g23.backend.dto.request.course.LearningPathCoursesRequest;
import fu.sep490.g23.backend.dto.request.course.LearningPathRequest;
import fu.sep490.g23.backend.dto.response.course.LearningPathResponse;
import fu.sep490.g23.backend.service.course.LearningPathManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content-manager/learning-paths")
@RequiredArgsConstructor
/** Exposes Content Manager APIs for creating and organizing learning paths. */
public class ContentManagerLearningPathController {
    private final LearningPathManagementService learningPathManagementService;

    @GetMapping
    /** Returns paginated learning paths for the management screen. */
    public ResponseEntity<Page<LearningPathResponse>> getPaths(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
        return ResponseEntity.ok(learningPathManagementService.getManagedPaths(pageable));
    }

    @PostMapping
    /** Creates the metadata for a new learning path. */
    public ResponseEntity<LearningPathResponse> createPath(@Valid @RequestBody LearningPathRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(learningPathManagementService.createPath(request));
    }

    @PutMapping("/{pathId}")
    /** Updates path metadata, target score, and discount policy. */
    public ResponseEntity<LearningPathResponse> updatePath(
            @PathVariable Long pathId,
            @Valid @RequestBody LearningPathRequest request
    ) {
        return ResponseEntity.ok(learningPathManagementService.updatePath(pathId, request));
    }

    @PostMapping("/{pathId}/courses")
    /** Appends courses that are not already part of the path. */
    public ResponseEntity<LearningPathResponse> addCourses(
            @PathVariable Long pathId,
            @Valid @RequestBody LearningPathCoursesRequest request
    ) {
        return ResponseEntity.ok(learningPathManagementService.addCourses(pathId, request));
    }

    @PutMapping("/{pathId}/courses/order")
    /** Persists the complete ordering of courses currently in the path. */
    public ResponseEntity<LearningPathResponse> reorderCourses(
            @PathVariable Long pathId,
            @Valid @RequestBody LearningPathCoursesRequest request
    ) {
        return ResponseEntity.ok(learningPathManagementService.reorderCourses(pathId, request));
    }

    @DeleteMapping("/{pathId}")
    /** Deletes the path without deleting referenced courses. */
    public ResponseEntity<Void> deletePath(@PathVariable Long pathId) {
        learningPathManagementService.deletePath(pathId);
        return ResponseEntity.noContent().build();
    }
}
