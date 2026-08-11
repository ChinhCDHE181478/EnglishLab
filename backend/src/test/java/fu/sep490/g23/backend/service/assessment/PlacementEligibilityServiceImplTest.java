package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.request.assessment.ReviewPlacementAttemptRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementEligibilityResult;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.service.assessment.impl.PlacementEligibilityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlacementEligibilityServiceImplTest {

    @Mock
    private PlacementTestAttemptRepository attemptRepository;

    @Mock
    private UserRepository userRepository;

    private PlacementEligibilityServiceImpl service;
    private User learner;

    @BeforeEach
    void setUp() {
        service = new PlacementEligibilityServiceImpl(attemptRepository, userRepository);
        learner = User.builder().id(10L).fullName("Học viên A").email("learner@example.com").build();
    }

    @Test
    void acceptsCompleteToeicAttempt() {
        PlacementTestAttempt attempt = baseAttempt()
                .aiFeedbackJson("{\"examType\":\"TOEIC\"}")
                .listeningScore(BigDecimal.valueOf(300))
                .readingScore(BigDecimal.valueOf(350))
                .overallScore(BigDecimal.valueOf(650))
                .evaluationStatus(PlacementEvaluationStatus.ELIGIBLE)
                .recommendedLevel(PlacementLevel.INTERMEDIATE)
                .build();
        when(attemptRepository.findById(1L)).thenReturn(Optional.of(attempt));

        PlacementEligibilityResult result = service.evaluateEligibility(learner.getId(), 1L);

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getStatus()).isEqualTo(PlacementEvaluationStatus.ELIGIBLE);
        assertThat(result.getMissingRequirements()).isEmpty();
    }

    @Test
    void requiresStaffReviewForIeltsAttempt() {
        PlacementTestAttempt attempt = baseAttempt()
                .aiFeedbackJson("{\"examType\":\"IELTS\"}")
                .listeningScore(BigDecimal.valueOf(5.5))
                .readingScore(BigDecimal.valueOf(6))
                .overallScore(BigDecimal.valueOf(5.75))
                .evaluationStatus(PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED)
                .build();
        when(attemptRepository.findById(1L)).thenReturn(Optional.of(attempt));

        PlacementEligibilityResult result = service.evaluateEligibility(learner.getId(), 1L);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getStatus()).isEqualTo(PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED);
        assertThat(result.getMissingRequirements()).contains("WRITING_REVIEW", "SPEAKING_REVIEW");
    }

    @Test
    void keepsExpiredStatusInsteadOfFallingBackToManualReview() {
        PlacementTestAttempt attempt = baseAttempt()
                .aiFeedbackJson("{\"examType\":\"IELTS\"}")
                .evaluationStatus(PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED)
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();
        when(attemptRepository.findById(1L)).thenReturn(Optional.of(attempt));

        PlacementEligibilityResult result = service.evaluateEligibility(learner.getId(), 1L);

        assertThat(result.isEligible()).isFalse();
        assertThat(result.getStatus()).isEqualTo(PlacementEvaluationStatus.EXPIRED);
        assertThat(result.getMissingRequirements()).contains("RESULT_EXPIRED");
    }

    @Test
    void staffReviewFinalizesRecommendedLevelAndAuditFields() {
        User staff = User.builder().id(20L).fullName("Nhân viên A").email("staff@example.com").build();
        staff.setRole(RoleEnum.STAFF);
        PlacementTestAttempt attempt = baseAttempt()
                .aiFeedbackJson("{\"examType\":\"IELTS\"}")
                .listeningScore(BigDecimal.valueOf(5.5))
                .readingScore(BigDecimal.valueOf(6))
                .overallScore(BigDecimal.valueOf(5.75))
                .evaluationStatus(PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED)
                .build();
        ReviewPlacementAttemptRequest request = new ReviewPlacementAttemptRequest();
        request.setRecommendedLevel(PlacementLevel.INTERMEDIATE);
        request.setNote("Đã kiểm tra phần Writing và Speaking.");
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(attemptRepository.findById(1L)).thenReturn(Optional.of(attempt));

        PlacementEligibilityResult result = service.confirmManualReview(1L, request, staff.getEmail());

        assertThat(result.isEligible()).isTrue();
        assertThat(result.getRecommendedLevel()).isEqualTo(PlacementLevel.INTERMEDIATE);
        assertThat(attempt.getReviewer()).isEqualTo(staff);
        assertThat(attempt.getReviewedAt()).isNotNull();
        verify(attemptRepository).save(attempt);
    }

    private PlacementTestAttempt.PlacementTestAttemptBuilder baseAttempt() {
        return PlacementTestAttempt.builder()
                .id(1L)
                .student(learner)
                .testCode("ENGLISHLAB_PLACEMENT_V1")
                .answersJson("{}")
                .status("COMPLETED")
                .submittedAt(LocalDateTime.now().minusDays(2))
                .expiresAt(LocalDateTime.now().plusDays(178));
    }
}
