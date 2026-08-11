package fu.sep490.g23.backend.controller.teacher;

import fu.sep490.g23.backend.dto.response.teacher.TeacherProfessionalResponse;
import fu.sep490.g23.backend.dto.response.teacher.TeacherFeedbackAggregateResponse;
import fu.sep490.g23.backend.service.teacher.TeacherFeedbackService;
import fu.sep490.g23.backend.service.teacher.TeacherProfessionalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teacher/professional-profile")
@RequiredArgsConstructor
public class TeacherProfessionalProfileController {

    private final TeacherProfessionalService teacherProfessionalService;
    private final TeacherFeedbackService teacherFeedbackService;

    @GetMapping
    public ResponseEntity<TeacherProfessionalResponse> myProfile(Authentication authentication) {
        return ResponseEntity.ok(teacherProfessionalService.getMyProfile(authentication.getName()));
    }

    @GetMapping("/feedback-summary")
    public ResponseEntity<TeacherFeedbackAggregateResponse> feedbackSummary(Authentication authentication) {
        return ResponseEntity.ok(teacherFeedbackService.getTeacherSummary(authentication.getName()));
    }
}
