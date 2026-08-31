package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.request.assessment.MockTestSubmissionRequest;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.MockTestAttemptRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.service.assessment.impl.MockTestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class MockTestServiceImplTest {

    @Mock private AssessmentBankItemRepository assessmentBankRepository;
    @Mock private MockTestAttemptRepository attemptRepository;
    @Mock private UserRepository userRepository;

    private MockTestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MockTestServiceImpl(
                assessmentBankRepository,
                attemptRepository,
                userRepository
        );
    }

    @Test
    void rejectsSubmissionWithoutAnyAnswer() {
        MockTestSubmissionRequest request = new MockTestSubmissionRequest();
        request.setObjectiveAnswersJson(" ");
        request.setSubmittedText("");
        request.setSubmittedAudioUrl(null);

        assertThatThrownBy(() -> service.submitMockTest(1L, request, "learner@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Bài thi chưa có câu trả lời để nộp.");
        verifyNoInteractions(assessmentBankRepository, attemptRepository, userRepository);
    }
}
