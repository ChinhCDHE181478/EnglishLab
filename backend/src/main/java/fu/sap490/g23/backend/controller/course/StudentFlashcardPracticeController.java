package fu.sap490.g23.backend.controller.course;

import fu.sap490.g23.backend.dto.response.course.VocabularyTermResponse;
import fu.sap490.g23.backend.entity.course.enums.FlashcardPracticeSource;
import fu.sap490.g23.backend.service.course.FlashcardPracticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/flashcards")
@RequiredArgsConstructor
public class StudentFlashcardPracticeController {
    private final FlashcardPracticeService flashcardPracticeService;

    @GetMapping("/practice")
    public ResponseEntity<List<VocabularyTermResponse>> getPracticeTerms(
            @RequestParam(defaultValue = "ENROLLED") FlashcardPracticeSource source,
            @RequestParam(required = false) Long courseId,
            @RequestParam(defaultValue = "false") boolean starredOnly,
            Authentication authentication
    ) {
        return ResponseEntity.ok(flashcardPracticeService.getPracticeTerms(source, courseId, starredOnly, authentication.getName()));
    }
}
