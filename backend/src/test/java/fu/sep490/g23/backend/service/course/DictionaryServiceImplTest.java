package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.request.course.SaveVocabularyRequest;
import fu.sep490.g23.backend.dto.response.course.SavedVocabularyResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.SavedVocabulary;
import fu.sep490.g23.backend.entity.course.enums.VocabularyMasteryStatus;
import fu.sep490.g23.backend.repository.course.SavedVocabularyRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.course.impl.DictionaryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DictionaryServiceImplTest {
    @Mock private SavedVocabularyRepository repository;
    @Mock private ClassroomAccessHelper accessHelper;
    @Mock private RestClient.Builder restClientBuilder;
    @Mock private DictionaryTranslationService dictionaryTranslationService;

    private DictionaryServiceImpl service;
    private User learner;

    @BeforeEach
    void setUp() {
        service = new DictionaryServiceImpl(repository, accessHelper, restClientBuilder, dictionaryTranslationService);
        learner = User.builder().id(21L).email("learner@test.vn").fullName("Learner").build();
        when(accessHelper.requireUser(learner.getEmail())).thenReturn(learner);
    }

    @Test
    void save_NormalizesWordAndCreatesLearningEntry() {
        SaveVocabularyRequest request = request("  Learning  ", "việc học");
        when(repository.findByUserIdAndWordIgnoreCase(21L, "learning")).thenReturn(Optional.empty());
        when(repository.save(any(SavedVocabulary.class))).thenAnswer(invocation -> {
            SavedVocabulary item = invocation.getArgument(0);
            item.setId(5L);
            return item;
        });

        SavedVocabularyResponse result = service.save(request, learner.getEmail());

        assertEquals("learning", result.getWord());
        assertEquals("việc học", result.getPrimaryDefinition());
        assertEquals(VocabularyMasteryStatus.LEARNING, result.getStatus());
    }

    @Test
    void save_ExistingWordUpdatesInsteadOfDuplicating() {
        SavedVocabulary existing = SavedVocabulary.builder()
                .id(7L).user(learner).word("focus").primaryDefinition("cũ")
                .status(VocabularyMasteryStatus.MASTERED).build();
        when(repository.findByUserIdAndWordIgnoreCase(21L, "focus")).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        SavedVocabularyResponse result = service.save(request("focus", "tập trung"), learner.getEmail());

        assertEquals(7L, result.getId());
        assertEquals("tập trung", result.getPrimaryDefinition());
        assertEquals(VocabularyMasteryStatus.MASTERED, result.getStatus());
        verify(repository).save(existing);
    }

    @Test
    void save_RejectsNonEnglishInputBeforeRepositoryLookup() {
        assertThrows(IllegalArgumentException.class,
                () -> service.save(request("học tập", "study"), learner.getEmail()));
        verify(repository, never()).findByUserIdAndWordIgnoreCase(anyLong(), anyString());
    }

    private SaveVocabularyRequest request(String word, String definition) {
        SaveVocabularyRequest request = new SaveVocabularyRequest();
        request.setWord(word);
        request.setPrimaryDefinition(definition);
        return request;
    }
}
