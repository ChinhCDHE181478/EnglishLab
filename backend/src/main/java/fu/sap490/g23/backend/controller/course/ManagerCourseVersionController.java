package fu.sap490.g23.backend.controller.course;

import fu.sap490.g23.backend.dto.request.course.ReviewCourseRequest;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseVersionResponse;
import fu.sap490.g23.backend.service.course.OnlineCourseVersionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/manager/online-courses/{courseId}/versions")
@RequiredArgsConstructor
public class ManagerCourseVersionController {
    private final OnlineCourseVersionService versionService;

    @PatchMapping("/{versionId}/publish")
    public ResponseEntity<OnlineCourseVersionResponse> publish(
            @PathVariable Long courseId,
            @PathVariable Long versionId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(versionService.publish(courseId, versionId, authentication.getName()));
    }

    @PatchMapping("/{versionId}/reject")
    public ResponseEntity<OnlineCourseVersionResponse> reject(
            @PathVariable Long courseId,
            @PathVariable Long versionId,
            @Valid @RequestBody ReviewCourseRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(versionService.reject(
                courseId,
                versionId,
                request.getReviewNote(),
                authentication.getName()
        ));
    }
}
