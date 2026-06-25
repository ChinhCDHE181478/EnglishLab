package fu.sap490.g23.backend.controller.course;

import fu.sap490.g23.backend.dto.request.course.ReviewCourseRequest;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.service.course.OnlineCourseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manager/courses")
@RequiredArgsConstructor
public class ManagerCourseController {

    private final OnlineCourseService onlineCourseService;

    @PostMapping("/{id}/approve")
    public ResponseEntity<OnlineCourseResponse> approveCourse(
            @PathVariable Long id,
            @Valid @RequestBody ReviewCourseRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlineCourseService.approveCourse(id, authentication.getName(), request.getReviewNote()));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<OnlineCourseResponse> rejectCourse(
            @PathVariable Long id,
            @Valid @RequestBody ReviewCourseRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlineCourseService.rejectCourse(id, authentication.getName(), request.getReviewNote()));
    }
}
