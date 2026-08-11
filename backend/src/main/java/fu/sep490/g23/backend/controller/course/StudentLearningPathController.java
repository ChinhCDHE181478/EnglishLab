package fu.sep490.g23.backend.controller.course;

import fu.sep490.g23.backend.dto.response.course.LearnerLearningPathResponse;
import fu.sep490.g23.backend.service.course.LearningPathManagementService;
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
    private final LearningPathManagementService learningPathManagementService;

    @GetMapping("/learning-path")
    public ResponseEntity<LearnerLearningPathResponse> getMyLearningPath(Authentication authentication) {
        return ResponseEntity.ok(learningPathManagementService.getMyLearningPath(authentication.getName()));
    }
}
