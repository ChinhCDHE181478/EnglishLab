package fu.sep490.g23.backend.controller.course;

import fu.sep490.g23.backend.dto.request.course.LearnerLessonNoteRequest;
import fu.sep490.g23.backend.dto.response.course.LearnerLessonNoteResponse;
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
    private final LearnerLearningExperienceService learningExperienceService;

    @GetMapping("/notes")
    public ResponseEntity<List<LearnerLessonNoteResponse>> getNotes(Authentication authentication) {
        return ResponseEntity.ok(learningExperienceService.getNotes(authentication.getName()));
    }

    @PostMapping("/courses/{courseId}/lessons/{lessonId}/notes")
    public ResponseEntity<LearnerLessonNoteResponse> createNote(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @Valid @RequestBody LearnerLessonNoteRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(learningExperienceService.createNote(courseId, lessonId, request, authentication.getName()));
    }

    @PutMapping("/notes/{noteId}")
    public ResponseEntity<LearnerLessonNoteResponse> updateNote(
            @PathVariable Long noteId,
            @Valid @RequestBody LearnerLessonNoteRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(learningExperienceService.updateNote(noteId, request, authentication.getName()));
    }

    @DeleteMapping("/notes/{noteId}")
    public ResponseEntity<Void> deleteNote(@PathVariable Long noteId, Authentication authentication) {
        learningExperienceService.deleteNote(noteId, authentication.getName());
        return ResponseEntity.noContent().build();
    }

}
