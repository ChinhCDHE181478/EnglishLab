package fu.sap490.g23.backend.controller.course;

import fu.sap490.g23.backend.dto.response.course.OnlineCourseVersionResponse;
import fu.sap490.g23.backend.service.course.OnlineCourseVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/manager/online-course-versions")
@RequiredArgsConstructor
public class ManagerCourseVersionReviewController {
    private final OnlineCourseVersionService versionService;

    @GetMapping("/pending")
    public ResponseEntity<List<OnlineCourseVersionResponse>> getPendingReviews(Authentication authentication) {
        return ResponseEntity.ok(versionService.getPendingReviews(authentication.getName()));
    }
}
