package fu.sap490.g23.backend.controller.course;

import fu.sap490.g23.backend.dto.request.course.OnlineCourseRequest;
import fu.sap490.g23.backend.dto.response.ApiResponse;
import fu.sap490.g23.backend.dto.response.course.BunnyVideoUploadResponse;
import fu.sap490.g23.backend.dto.response.course.CourseStatsResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.entity.course.CourseCategoryCode;
import fu.sap490.g23.backend.entity.course.PackageStatus;
import fu.sap490.g23.backend.service.course.OnlineCourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/content-manager/online-courses")
@RequiredArgsConstructor
public class ContentManagerOnlineCourseController {

    private final OnlineCourseService onlineCourseService;

    @GetMapping
    public ResponseEntity<Page<OnlineCourseResponse>> getCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CourseCategoryCode category,
            @RequestParam(required = false) PackageStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(onlineCourseService.getManagerCourses(keyword, category, status, pageable));
    }

    @GetMapping("/{slugOrId}")
    public ResponseEntity<OnlineCourseResponse> getCourse(@PathVariable String slugOrId) {
        return ResponseEntity.ok(onlineCourseService.getManagerCourse(slugOrId));
    }

    @GetMapping("/stats")
    public ResponseEntity<CourseStatsResponse> getStats() {
        return ResponseEntity.ok(onlineCourseService.getStats());
    }

    @PostMapping
    public ResponseEntity<OnlineCourseResponse> createCourse(@Valid @RequestBody OnlineCourseRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onlineCourseService.createCourse(request, authentication.getName()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<OnlineCourseResponse> updateCourse(@PathVariable Long id, @Valid @RequestBody OnlineCourseRequest request) {
        return ResponseEntity.ok(onlineCourseService.updateCourse(id, request));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<OnlineCourseResponse> publishCourse(@PathVariable Long id) {
        return ResponseEntity.ok(onlineCourseService.publishCourse(id));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<OnlineCourseResponse> archiveCourse(@PathVariable Long id) {
        return ResponseEntity.ok(onlineCourseService.archiveCourse(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteCourse(@PathVariable Long id) {
        onlineCourseService.deleteCourse(id);
        return ResponseEntity.ok(ApiResponse.builder()
                .message("Online course deleted successfully")
                .description("The package was soft-deleted and archived.")
                .build());
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/bunny-video")
    public ResponseEntity<BunnyVideoUploadResponse> uploadLessonVideo(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @RequestParam(required = false) String title,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(onlineCourseService.uploadLessonVideo(courseId, lessonId, title, file));
    }
}
