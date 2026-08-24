package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.request.course.SaveVocabularyRequest;
import fu.sep490.g23.backend.dto.request.course.UpdateSavedVocabularyRequest;
import fu.sep490.g23.backend.dto.response.course.DictionaryEntryResponse;
import fu.sep490.g23.backend.dto.response.course.SavedVocabularyResponse;
import fu.sep490.g23.backend.entity.course.enums.VocabularyMasteryStatus;

import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DictionaryService {

    DictionaryEntryResponse lookup(String word);

    List<SavedVocabularyResponse> listSaved(String userEmail, String keyword, VocabularyMasteryStatus status);

    Page<SavedVocabularyResponse> pageSaved(String userEmail, String keyword, VocabularyMasteryStatus status, Pageable pageable);

    Map<String, Long> getSavedStats(String userEmail);

    boolean isSaved(String userEmail, String word);

    SavedVocabularyResponse save(SaveVocabularyRequest request, String userEmail);

    SavedVocabularyResponse update(Long savedVocabularyId, UpdateSavedVocabularyRequest request, String userEmail);

    void delete(Long savedVocabularyId, String userEmail);
}
