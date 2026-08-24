package fu.sep490.g23.backend.controller.course;

import fu.sep490.g23.backend.dto.request.course.SaveVocabularyRequest;
import fu.sep490.g23.backend.dto.request.course.UpdateSavedVocabularyRequest;
import fu.sep490.g23.backend.dto.response.course.DictionaryEntryResponse;
import fu.sep490.g23.backend.dto.response.course.SavedVocabularyResponse;
import fu.sep490.g23.backend.entity.course.enums.VocabularyMasteryStatus;
import fu.sep490.g23.backend.service.course.DictionaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @GetMapping("/saved/page")
    public ResponseEntity<Page<SavedVocabularyResponse>> pageSaved(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) VocabularyMasteryStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "6") int size,
            Authentication authentication
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(dictionaryService.pageSaved(
                authentication.getName(),
                keyword,
                status,
                PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "updatedAt"))
        ));
    }

    @GetMapping("/saved/stats")
    public ResponseEntity<Map<String, Long>> getSavedStats(Authentication authentication) {
        return ResponseEntity.ok(dictionaryService.getSavedStats(authentication.getName()));
    }

    @GetMapping("/saved/contains")
    public ResponseEntity<Map<String, Boolean>> isSaved(
            @RequestParam String word,
            Authentication authentication
    ) {
        return ResponseEntity.ok(Map.of("saved", dictionaryService.isSaved(authentication.getName(), word)));
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
