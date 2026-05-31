package fu.sap490.g23.backend.controller.course;

import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.dto.response.course.PackageEnrollmentResponse;
import fu.sap490.g23.backend.service.course.OnlineCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/online-courses")
@RequiredArgsConstructor
public class StudentOnlineCourseController {

    private final OnlineCourseService onlineCourseService;

    @PostMapping("/{courseId}/register")
    public ResponseEntity<OnlineCourseResponse> registerCourse(@PathVariable Long courseId, Authentication authentication) {
        return ResponseEntity.ok(onlineCourseService.registerCourse(courseId, authentication.getName()));
    }

    @GetMapping({"/my-enrollments", "/my-courses"})
    public ResponseEntity<List<PackageEnrollmentResponse>> getMyEnrollments(Authentication authentication) {
        return ResponseEntity.ok(onlineCourseService.getMyEnrollments(authentication.getName()));
    }

    @PatchMapping("/{courseId}/lessons/{lessonId}/progress")
    public ResponseEntity<PackageEnrollmentResponse> updateLessonProgress(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @RequestParam(defaultValue = "true") boolean completed,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlineCourseService.updateLessonProgress(courseId, lessonId, completed, authentication.getName()));
    }
}
