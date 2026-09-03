package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.request.assessment.MockTestSubmissionRequest;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.MockTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import fu.sep490.g23.backend.entity.curriculum.AssessmentBankItem;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.MockTestAttemptRepository;
import fu.sep490.g23.backend.repository.curriculum.AssessmentBankItemRepository;
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.ai.AiEvaluationResult;
import fu.sep490.g23.backend.service.assessment.impl.MockTestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MockTestServiceImplTest {

    @Mock private AssessmentBankItemRepository assessmentBankRepository;
    @Mock private MockTestAttemptRepository attemptRepository;
    @Mock private UserRepository userRepository;
    @Mock private AiEvaluationClient aiEvaluationClient;
    @Mock private AssessmentAudioStorageService audioStorageService;

    private MockTestServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MockTestServiceImpl(
                assessmentBankRepository,
                attemptRepository,
                userRepository,
                aiEvaluationClient,
                audioStorageService
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

    @Test
    void gradesWritingMockTestWithAiAndPersistsFeedback() {
        AssessmentBankItem mockTest = AssessmentBankItem.builder()
                .id(10L)
                .title("IELTS Writing Mock Test")
                .type(AssessmentType.MOCK_TEST)
                .skill(AssessmentSkill.WRITING)
                .maxScore(BigDecimal.valueOf(9))
                .build();
        User student = User.builder().id(5L).email("learner@example.com").build();
        MockTestSubmissionRequest request = new MockTestSubmissionRequest();
        request.setSubmittedText("This essay presents a clear position and supports it with relevant examples.");

        when(assessmentBankRepository.findByIdAndTypeAndStatus(10L, AssessmentType.MOCK_TEST, "PUBLISHED"))
                .thenReturn(Optional.of(mockTest));
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(aiEvaluationClient.evaluate(anyString())).thenReturn(AiEvaluationResult.builder()
                .estimatedScore(BigDecimal.valueOf(7.5))
                .feedbackJson("{\"summary\":\"Lập luận rõ ràng.\"}")
                .build());
        when(attemptRepository.save(any(MockTestAttempt.class))).thenAnswer(invocation -> {
            MockTestAttempt attempt = invocation.getArgument(0);
            attempt.setId(99L);
            return attempt;
        });

        var response = service.submitMockTest(10L, request, student.getEmail());

        assertThat(response.getScore()).isEqualByComparingTo("7.50");
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        assertThat(response.getAiFeedbackJson()).contains("Lập luận rõ ràng");
        verify(aiEvaluationClient).evaluate(anyString());
    }

    @Test
    void gradesSpeakingMockTestFromStoredAudio() {
        AssessmentBankItem mockTest = AssessmentBankItem.builder()
                .id(11L)
                .title("IELTS Speaking Mock Test")
                .type(AssessmentType.MOCK_TEST)
                .skill(AssessmentSkill.SPEAKING)
                .maxScore(BigDecimal.valueOf(9))
                .build();
        User student = User.builder().id(6L).email("speaker@example.com").build();
        MockTestSubmissionRequest request = new MockTestSubmissionRequest();
        request.setSubmittedAudioUrl("/api/assessment-audio/speaking.webm");
        request.setSubmittedText("Speaking mock test metadata");
        byte[] audio = new byte[]{1, 2, 3};

        when(assessmentBankRepository.findByIdAndTypeAndStatus(11L, AssessmentType.MOCK_TEST, "PUBLISHED"))
                .thenReturn(Optional.of(mockTest));
        when(userRepository.findByEmail(student.getEmail())).thenReturn(Optional.of(student));
        when(audioStorageService.loadStoredAudioFromUrl(request.getSubmittedAudioUrl()))
                .thenReturn(Optional.of(new AssessmentAudioStorageService.StoredAssessmentAudio(
                        "speaking.webm", "audio/webm", audio.length, audio)));
        when(aiEvaluationClient.evaluateWithAudio(anyString(), any(byte[].class), anyString()))
                .thenReturn(AiEvaluationResult.builder()
                        .estimatedScore(BigDecimal.valueOf(6.5))
                        .feedbackJson("{\"summary\":\"Phát âm khá rõ.\"}")
                        .build());
        when(attemptRepository.save(any(MockTestAttempt.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.submitMockTest(11L, request, student.getEmail());

        assertThat(response.getScore()).isEqualByComparingTo("6.50");
        verify(aiEvaluationClient).evaluateWithAudio(anyString(), any(byte[].class), anyString());
    }
}
