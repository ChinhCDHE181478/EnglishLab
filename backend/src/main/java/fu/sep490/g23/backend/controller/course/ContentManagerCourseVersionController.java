package fu.sep490.g23.backend.controller.course;

import fu.sep490.g23.backend.dto.request.course.CreateCourseVersionRequest;
import fu.sep490.g23.backend.dto.request.course.OnlineCourseRequest;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseVersionResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCoursePreviewResponse;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/content-manager/online-courses/{courseId}/versions")
@RequiredArgsConstructor
public class ContentManagerCourseVersionController {
    private final OnlineCourseVersionService versionService;
    private final OnlineCourseService onlineCourseService;

    @GetMapping
    public ResponseEntity<List<OnlineCourseVersionResponse>> getVersions(
            @PathVariable Long courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(versionService.getVersions(courseId, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<OnlineCourseVersionResponse> createDraft(
            @PathVariable Long courseId,
            @Valid @RequestBody(required = false) CreateCourseVersionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(versionService.createDraft(courseId, request, authentication.getName()));
    }

    @GetMapping("/{versionId}")
    public ResponseEntity<OnlineCourseVersionResponse> getVersion(
            @PathVariable Long courseId,
            @PathVariable Long versionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(versionService.getVersion(courseId, versionId, authentication.getName()));
    }

    @GetMapping("/{versionId}/preview")
    public ResponseEntity<OnlineCoursePreviewResponse> getVersionPreview(
            @PathVariable Long courseId,
            @PathVariable Long versionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(versionService.getVersionPreview(courseId, versionId, authentication.getName()));
    }

    @PutMapping("/{versionId}")
    public ResponseEntity<OnlineCourseVersionResponse> updateVersion(
            @PathVariable Long courseId,
            @PathVariable Long versionId,
            @Valid @RequestBody OnlineCourseRequest request,
            Authentication authentication
    ) {
        OnlineCourseVersionResponse current = versionService.getVersion(
                courseId,
                versionId,
                authentication.getName()
        );
        if (current.getStatus() != CourseVersionStatus.DRAFT) {
            throw new IllegalStateException("Chỉ phiên bản nháp mới có thể chỉnh sửa.");
        }
        onlineCourseService.updateCourse(courseId, request, authentication.getName());
        return ResponseEntity.ok(versionService.getVersion(courseId, versionId, authentication.getName()));
    }

    @PatchMapping("/{versionId}/publish")
    public ResponseEntity<OnlineCourseVersionResponse> publish(
            @PathVariable Long courseId,
            @PathVariable Long versionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(versionService.publish(courseId, versionId, authentication.getName()));
    }
}
