package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CreateClassroomQuizRequest;
import fu.sap490.g23.backend.dto.request.classroom.SubmitClassroomQuizRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomQuizResponse;
import fu.sap490.g23.backend.service.classroom.ClassroomQuizService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ClassroomQuizController {

    private final ClassroomQuizService quizService;

    @GetMapping("/api/teacher/classrooms/{offeringId}/quizzes")
    public ResponseEntity<List<ClassroomQuizResponse>> listForTeacher(
            @PathVariable Long offeringId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizService.listForClass(offeringId, authentication.getName()));
    }

    @PostMapping("/api/teacher/classrooms/{offeringId}/quizzes")
    public ResponseEntity<ClassroomQuizResponse> create(
            @PathVariable Long offeringId,
            @Valid @RequestBody CreateClassroomQuizRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizService.create(offeringId, request, authentication.getName()));
    }

    @PatchMapping("/api/teacher/quizzes/{quizId}/open")
    public ResponseEntity<ClassroomQuizResponse> open(@PathVariable Long quizId) {
        return ResponseEntity.ok(quizService.open(quizId));
    }

    @PatchMapping("/api/teacher/quizzes/{quizId}/close")
    public ResponseEntity<ClassroomQuizResponse> close(@PathVariable Long quizId) {
        return ResponseEntity.ok(quizService.close(quizId));
    }

    @DeleteMapping("/api/teacher/quizzes/{quizId}")
    public ResponseEntity<Void> delete(@PathVariable Long quizId) {
        quizService.delete(quizId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/student/classrooms/quizzes")
    public ResponseEntity<List<ClassroomQuizResponse>> listForLearner(Authentication authentication) {
        return ResponseEntity.ok(quizService.listForLearner(authentication.getName()));
    }

    @PostMapping("/api/student/quizzes/{quizId}/submit")
    public ResponseEntity<ClassroomQuizResponse> submit(
            @PathVariable Long quizId,
            @Valid @RequestBody SubmitClassroomQuizRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(quizService.submit(quizId, request, authentication.getName()));
    }
}
