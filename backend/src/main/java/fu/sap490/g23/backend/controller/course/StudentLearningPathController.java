package fu.sap490.g23.backend.controller.course;

import fu.sap490.g23.backend.dto.response.course.LearnerLearningPathResponse;
import fu.sap490.g23.backend.service.course.OnlineCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentLearningPathController {
    private final OnlineCourseService onlineCourseService;

    @GetMapping("/learning-path")
    public ResponseEntity<LearnerLearningPathResponse> getMyLearningPath(Authentication authentication) {
        return ResponseEntity.ok(onlineCourseService.getMyLearningPath(authentication.getName()));
    }
}
