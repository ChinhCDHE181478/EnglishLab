package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.request.assessment.UpsertExerciseBankItemRequest;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.ExerciseBankItemRepository;
import fu.sep490.g23.backend.service.assessment.impl.ExerciseBankServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class ExerciseBankServiceImplTest {

    private ExerciseBankItemRepository repository;
    private UserRepository userRepository;
    private ExerciseBankServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(ExerciseBankItemRepository.class);
        userRepository = mock(UserRepository.class);
        service = new ExerciseBankServiceImpl(repository, userRepository);
    }

    @Test
    void createRejectsPlainTextListeningPractice() {
        UpsertExerciseBankItemRequest request = practiceRequest("Nội dung nhập tự do", "{\"1\":\"B\"}");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request, "content@englishlab.vn"));

        assertEquals(
                "Bài luyện tập Listening/Reading phải được biên soạn bằng trình làm bài trên hệ thống.",
                exception.getMessage());
        verifyNoInteractions(repository, userRepository);
    }

    @Test
    void createRejectsSystemPracticeWithoutAutomaticAnswerKey() {
        UpsertExerciseBankItemRequest request = practiceRequest(
                "{\"parts\":[{\"key\":\"part_1\"}]}",
                "{}");

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.create(request, "content@englishlab.vn"));

        assertEquals("Bài luyện tập phải có đáp án chấm tự động.", exception.getMessage());
        verifyNoInteractions(repository, userRepository);
    }

    private UpsertExerciseBankItemRequest practiceRequest(String prompt, String answerKey) {
        UpsertExerciseBankItemRequest request = new UpsertExerciseBankItemRequest();
        request.setTitle("TOEIC Unit 1 Practice");
        request.setSkill("LISTENING");
        request.setExerciseType("PRACTICE");
        request.setPrompt(prompt);
        request.setAnswerKey(answerKey);
        return request;
    }
}
