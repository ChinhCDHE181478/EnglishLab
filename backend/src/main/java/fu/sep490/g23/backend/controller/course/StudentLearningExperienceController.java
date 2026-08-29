package fu.sep490.g23.backend.controller.course;

import fu.sep490.g23.backend.dto.request.course.LearnerLessonNoteRequest;
import fu.sep490.g23.backend.dto.response.course.LearnerLessonNoteResponse;
import fu.sep490.g23.backend.dto.response.course.LearnerLessonReviewFlagResponse;
import fu.sep490.g23.backend.service.course.LearnerLearningExperienceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/learning")
@RequiredArgsConstructor
public class StudentLearningExperienceController {

}
