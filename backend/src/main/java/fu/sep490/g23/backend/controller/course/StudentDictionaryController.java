package fu.sap490.g23.backend.controller.course;

import fu.sap490.g23.backend.dto.request.course.SaveVocabularyRequest;
import fu.sap490.g23.backend.dto.request.course.UpdateSavedVocabularyRequest;
import fu.sap490.g23.backend.dto.response.course.DictionaryEntryResponse;
import fu.sap490.g23.backend.dto.response.course.SavedVocabularyResponse;
import fu.sap490.g23.backend.entity.course.enums.VocabularyMasteryStatus;
import fu.sap490.g23.backend.service.course.DictionaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student/dictionary")
@RequiredArgsConstructor
public class StudentDictionaryController {

    private final DictionaryService dictionaryService;

    @GetMapping("/lookup")
    public ResponseEntity<DictionaryEntryResponse> lookup(@RequestParam String word) {
        return ResponseEntity.ok(dictionaryService.lookup(word));
    }

    @GetMapping("/saved")
    public ResponseEntity<List<SavedVocabularyResponse>> listSaved(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) VocabularyMasteryStatus status,
            Authentication authentication
    ) {
        return ResponseEntity.ok(dictionaryService.listSaved(authentication.getName(), keyword, status));
    }

    @PostMapping("/saved")
    public ResponseEntity<SavedVocabularyResponse> save(
            @Valid @RequestBody SaveVocabularyRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(dictionaryService.save(request, authentication.getName()));
    }

    @PutMapping("/saved/{savedVocabularyId}")
    public ResponseEntity<SavedVocabularyResponse> update(
            @PathVariable Long savedVocabularyId,
            @Valid @RequestBody UpdateSavedVocabularyRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(dictionaryService.update(savedVocabularyId, request, authentication.getName()));
    }

    @DeleteMapping("/saved/{savedVocabularyId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long savedVocabularyId,
            Authentication authentication
    ) {
        dictionaryService.delete(savedVocabularyId, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
