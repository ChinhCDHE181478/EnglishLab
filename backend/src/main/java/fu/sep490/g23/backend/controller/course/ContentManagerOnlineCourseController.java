package fu.sep490.g23.backend.controller.course;

import fu.sep490.g23.backend.dto.request.assessment.ContentManagerCourseAssessmentRequest;
import fu.sep490.g23.backend.dto.request.course.OnlineCourseRequest;
import fu.sep490.g23.backend.dto.request.course.ReorderLessonsRequest;
import fu.sep490.g23.backend.dto.request.course.ReorderModulesRequest;
import fu.sep490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sep490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import fu.sep490.g23.backend.dto.response.ApiResponse;
import fu.sep490.g23.backend.dto.response.course.BunnyVideoUploadResponse;
import fu.sep490.g23.backend.dto.response.course.CourseThumbnailUploadResponse;
import fu.sep490.g23.backend.dto.response.course.CourseStatsResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCoursePreviewResponse;
import fu.sep490.g23.backend.dto.response.course.LessonResponse;
import fu.sep490.g23.backend.dto.response.course.ModuleResponse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import fu.sep490.g23.backend.service.course.CourseThumbnailStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/content-manager/online-courses")
@RequiredArgsConstructor
public class ContentManagerOnlineCourseController {

    private final OnlineCourseService onlineCourseService;
    private final CourseThumbnailStorageService courseThumbnailStorageService;

    @GetMapping
    public ResponseEntity<Page<OnlineCourseResponse>> getCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) CourseLevel level,
            @RequestParam(required = false) PackageStatus status,
            @RequestParam(required = false) String excludeIds,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort));
        return ResponseEntity.ok(onlineCourseService.getManagerCourses(
                keyword,
                category,
                level,
                status,
                parseIds(excludeIds),
                pageable
        ));
    }

    private Set<Long> parseIds(String value) {
        if (value == null || value.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .map(Long::valueOf)
                .collect(Collectors.toSet());
    }

    private Sort resolveSort(String value) {
        return switch (value == null ? "newest" : value) {
            case "oldest" -> Sort.by("id").ascending();
            case "titleAsc" -> Sort.by("title").ascending();
            case "titleDesc" -> Sort.by("title").descending();
            case "priceAsc" -> Sort.by("price").ascending();
            case "priceDesc" -> Sort.by("price").descending();
            default -> Sort.by("id").descending();
        };
    }

    /**
     * Retrieves full online course structure including version history, modules, lessons, and linked assessments.
     */
    @GetMapping("/{slugOrId}")
    public ResponseEntity<OnlineCourseResponse> getCourse(@PathVariable String slugOrId) {
        return ResponseEntity.ok(onlineCourseService.getManagerCourse(slugOrId));
    }

    @GetMapping("/{slugOrId}/preview")
    public ResponseEntity<OnlineCoursePreviewResponse> getCoursePreview(@PathVariable String slugOrId) {
        return ResponseEntity.ok(onlineCourseService.getManagerCoursePreview(slugOrId));
    }

    /**
     * Reorders modules sequentially within the editable draft course version.
     */
    @PatchMapping("/{courseId}/modules/reorder")
    public ResponseEntity<List<ModuleResponse>> reorderModules(
            @PathVariable Long courseId,
            @Valid @RequestBody ReorderModulesRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlineCourseService.reorderModules(courseId, request, authentication.getName()));
    }

    /**
     * Reorders lessons sequentially within a specific course module.
     */
    @PatchMapping("/{courseId}/modules/{moduleId}/lessons/reorder")
    public ResponseEntity<List<LessonResponse>> reorderLessons(
            @PathVariable Long courseId,
            @PathVariable Long moduleId,
            @Valid @RequestBody ReorderLessonsRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlineCourseService.reorderLessons(
                courseId,
                moduleId,
                request,
                authentication.getName()
        ));
    }

    @GetMapping("/{courseId}/assessments")
    public ResponseEntity<List<CourseAssessmentResponse>> getCourseAssessments(@PathVariable Long courseId) {
        return ResponseEntity.ok(onlineCourseService.getManagerCourseAssessments(courseId));
    }

    /**
     * Batch synchronizes course assessment items across modules and lessons.
     * Iterates through the incoming list, resolves bank items and rubrics, validates configuration,
     * updates active CourseAssessment records, and captures a new draft snapshot.
     */
    @PutMapping("/{courseId}/assessments")
    public ResponseEntity<List<CourseAssessmentResponse>> saveCourseAssessments(
            @PathVariable Long courseId,
            @Valid @RequestBody List<ContentManagerCourseAssessmentRequest> requests,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlineCourseService.saveManagerCourseAssessments(
                courseId,
                requests,
                authentication.getName()
        ));
    }

    @GetMapping("/assessment-rubrics")
    public ResponseEntity<List<AssessmentRubricResponse>> getAssessmentRubrics() {
        return ResponseEntity.ok(onlineCourseService.getManagerAssessmentRubrics());
    }

    @GetMapping("/stats")
    public ResponseEntity<CourseStatsResponse> getStats() {
        return ResponseEntity.ok(onlineCourseService.getStats());
    }

    @PostMapping(value = "/thumbnail", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CourseThumbnailUploadResponse> uploadThumbnail(@RequestPart("file") MultipartFile file) {
        String fileName = courseThumbnailStorageService.store(file);
        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/course-thumbnails/")
                .path(fileName)
                .toUriString();
        return ResponseEntity.ok(new CourseThumbnailUploadResponse(url));
    }

    /**
     * Creates a new online course and initializes its first draft version (V1 DRAFT).
     * Validates slug uniqueness, active category existence, and default pricing.
     */
    @PostMapping
    public ResponseEntity<OnlineCourseResponse> createCourse(@Valid @RequestBody OnlineCourseRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(onlineCourseService.createCourse(request, authentication.getName()));
    }

    /**
     * Updates course metadata and pricing, ensuring modifications only apply to editable draft versions.
     */
    @PutMapping("/{id}")
    public ResponseEntity<OnlineCourseResponse> updateCourse(
            @PathVariable Long id,
            @Valid @RequestBody OnlineCourseRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlineCourseService.updateCourse(id, request, authentication.getName()));
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<OnlineCourseResponse> publishCourse(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(onlineCourseService.publishCourse(id, authentication.getName()));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<OnlineCourseResponse> archiveCourse(@PathVariable Long id) {
        return ResponseEntity.ok(onlineCourseService.archiveCourse(id));
    }

    /**
     * Soft-deletes / archives an online course to safely preserve enrollment history.
     */
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
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlineCourseService.uploadLessonVideo(
                courseId,
                lessonId,
                title,
                file,
                authentication.getName()
        ));
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/transcript/youtube")
    public ResponseEntity<OnlineCourseResponse> refreshLessonTranscript(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlineCourseService.refreshLessonTranscript(
                courseId,
                lessonId,
                authentication.getName()
        ));
    }
}
