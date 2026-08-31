package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.response.assessment.PlacementEligibilityResult;
import fu.sep490.g23.backend.dto.response.assessment.PlacementRecommendationResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.service.assessment.impl.PlacementRecommendationServiceImpl;
import fu.sep490.g23.backend.service.course.LearningPathRecommendationService;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlacementRecommendationServiceImplTest {
    @Mock private UserRepository userRepository;
    @Mock private PlacementTestAttemptRepository attemptRepository;
    @Mock private PlacementEligibilityService eligibilityService;
    @Mock private OnlineCourseService onlineCourseService;
    @Mock private InstructorLedCourseRepository instructorLedCourseRepository;
    @Mock private LearningPathRecommendationService learningPathRecommendationService;

    private PlacementRecommendationServiceImpl service;
    private User learner;
    private PlacementTestAttempt attempt;

    @BeforeEach
    void setUp() {
        learner = User.builder().id(1L).email("learner@example.com").targetExam("IELTS").targetScore("6.5").currentBand(4D).build();
        attempt = PlacementTestAttempt.builder().id(10L).student(learner).testCode("IELTS_PLACEMENT")
                .answersJson("{}").status("COMPLETED").submittedAt(LocalDateTime.now())
                .aiFeedbackJson("{\"examType\":\"IELTS\"}").overallScore(BigDecimal.valueOf(5.5))
                .listeningScore(BigDecimal.valueOf(5.5)).readingScore(BigDecimal.valueOf(5))
                .writingScore(BigDecimal.valueOf(4.5)).speakingScore(BigDecimal.valueOf(5)).build();
        service = new PlacementRecommendationServiceImpl(userRepository, attemptRepository, eligibilityService,
                new PlacementRecommendationContextFactory(), onlineCourseService, instructorLedCourseRepository,
                learningPathRecommendationService);
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(attemptRepository.findById(10L)).thenReturn(Optional.of(attempt));
    }

    @Test
    void manualReviewPendingBuildsRecommendationsWhenBandIsAvailable() {
        when(eligibilityService.evaluateEligibility(1L, 10L)).thenReturn(eligibility(false, PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED));
        when(onlineCourseService.recommendCourses(org.mockito.ArgumentMatchers.eq(learner), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(instructorLedCourseRepository.findAllByOrderByUpdatedAtDescIdDesc()).thenReturn(List.of());

        PlacementRecommendationResponse result = service.getRecommendations(10L, learner.getEmail());

        assertThat(result.isRecommendationReady()).isTrue();
        assertThat(result.getOverallScore()).isEqualByComparingTo("5.5");
        verify(onlineCourseService).recommendCourses(org.mockito.ArgumentMatchers.eq(learner), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void manualReviewPendingStillWaitsWhenBandIsMissing() {
        attempt.setOverallScore(null);
        when(eligibilityService.evaluateEligibility(1L, 10L)).thenReturn(eligibility(false, PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED));

        PlacementRecommendationResponse result = service.getRecommendations(10L, learner.getEmail());

        assertThat(result.isRecommendationReady()).isFalse();
        verify(onlineCourseService, never()).recommendCourses(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void eligibleAttemptUsesAttemptScoreAndBuildsRecommendations() {
        when(eligibilityService.evaluateEligibility(1L, 10L)).thenReturn(eligibility(true, PlacementEvaluationStatus.ELIGIBLE));
        when(onlineCourseService.recommendCourses(org.mockito.ArgumentMatchers.eq(learner), org.mockito.ArgumentMatchers.any())).thenReturn(List.of());
        when(instructorLedCourseRepository.findAllByOrderByUpdatedAtDescIdDesc()).thenReturn(List.of());

        PlacementRecommendationResponse result = service.getRecommendations(10L, learner.getEmail());

        assertThat(result.isRecommendationReady()).isTrue();
        assertThat(result.getOverallScore()).isEqualByComparingTo("5.5");
        assertThat(result.getOverallScore()).isNotEqualByComparingTo("4.0");
        assertThat(result.getWeakSkills()).contains("WRITING");
    }

    private PlacementEligibilityResult eligibility(boolean eligible, PlacementEvaluationStatus status) {
        return PlacementEligibilityResult.builder().attemptId(10L).eligible(eligible).status(status)
                .recommendedLevel(eligible ? PlacementLevel.INTERMEDIATE : null).missingRequirements(List.of()).build();
    }
}
