package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.SaveVocabularyRequest;
import fu.sap490.g23.backend.dto.request.course.UpdateSavedVocabularyRequest;
import fu.sap490.g23.backend.dto.response.course.DictionaryEntryResponse;
import fu.sap490.g23.backend.dto.response.course.SavedVocabularyResponse;
import fu.sap490.g23.backend.entity.course.enums.VocabularyMasteryStatus;

import java.util.List;

public interface DictionaryService {

    DictionaryEntryResponse lookup(String word);

    List<SavedVocabularyResponse> listSaved(String userEmail, String keyword, VocabularyMasteryStatus status);

    SavedVocabularyResponse save(SaveVocabularyRequest request, String userEmail);

    SavedVocabularyResponse update(Long savedVocabularyId, UpdateSavedVocabularyRequest request, String userEmail);

    void delete(Long savedVocabularyId, String userEmail);
}
