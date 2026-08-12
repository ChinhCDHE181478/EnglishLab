package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PlacementRecommendationContextFactoryTest {
    private final PlacementRecommendationContextFactory factory = new PlacementRecommendationContextFactory();

    @Test
    void usesAttemptScoreInsteadOfStaleProfileBand() {
        User learner = User.builder().id(1L).targetExam("IELTS").targetScore("6.5").currentBand(4D).build();
        PlacementTestAttempt attempt = PlacementTestAttempt.builder()
                .id(10L)
                .student(learner)
                .overallScore(BigDecimal.valueOf(5.5))
                .listeningScore(BigDecimal.valueOf(5.5))
                .readingScore(BigDecimal.valueOf(5))
                .writingScore(BigDecimal.valueOf(4.5))
                .speakingScore(BigDecimal.valueOf(5))
                .aiFeedbackJson("{\"examType\":\"IELTS\"}")
                .build();

        PlacementRecommendationContext context = factory.fromAttempt(learner, attempt, PlacementLevel.INTERMEDIATE);

        assertThat(context.getOverallScore()).isEqualByComparingTo("5.5");
        assertThat(context.getWeakSkills()).contains(AssessmentSkill.WRITING);
    }

    @Test
    void toeicWeakSkillsNeverContainWritingOrSpeaking() {
        User learner = User.builder().id(1L).targetExam("TOEIC").targetScore("750").build();
        PlacementTestAttempt attempt = PlacementTestAttempt.builder()
                .id(11L)
                .student(learner)
                .overallScore(BigDecimal.valueOf(590))
                .listeningScore(BigDecimal.valueOf(310))
                .readingScore(BigDecimal.valueOf(280))
                .writingScore(BigDecimal.ONE)
                .speakingScore(BigDecimal.ONE)
                .aiFeedbackJson("{\"examType\":\"TOEIC\"}")
                .build();

        PlacementRecommendationContext context = factory.fromAttempt(learner, attempt, PlacementLevel.INTERMEDIATE);

        assertThat(context.getWeakSkills()).containsExactly(AssessmentSkill.READING);
        assertThat(context.getWeakSkills()).doesNotContain(AssessmentSkill.WRITING, AssessmentSkill.SPEAKING);
    }
}
