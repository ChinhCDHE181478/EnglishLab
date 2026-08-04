package fu.sap490.g23.backend.controller.teacher;

import fu.sap490.g23.backend.dto.request.teacher.UpsertTeacherCourseFeedbackRequest;
import fu.sap490.g23.backend.dto.response.teacher.LearnerTeacherFeedbackResponse;
import fu.sap490.g23.backend.service.teacher.TeacherFeedbackService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/classrooms/{classroomId}/teacher-feedback")
@RequiredArgsConstructor
public class StudentTeacherFeedbackController {
    private final TeacherFeedbackService teacherFeedbackService;

    @GetMapping
    public ResponseEntity<List<LearnerTeacherFeedbackResponse>> forms(
            @PathVariable Long classroomId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(teacherFeedbackService.getLearnerForms(classroomId, authentication.getName()));
    }

    @PutMapping("/{teacherId}")
    public ResponseEntity<LearnerTeacherFeedbackResponse> save(
            @PathVariable Long classroomId,
            @PathVariable Long teacherId,
            @Valid @RequestBody UpsertTeacherCourseFeedbackRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(
                teacherFeedbackService.saveLearnerFeedback(classroomId, teacherId, authentication.getName(), request)
        );
    }
}
