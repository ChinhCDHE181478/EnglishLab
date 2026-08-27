package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.Set;

/** Input bag passed to course / program / learning-path recommenders. */
@Value
@Builder
public class PlacementRecommendationContext {
    Long learnerId;
    Long attemptId;
    String examType;
    BigDecimal overallScore;
    BigDecimal listeningScore;
    BigDecimal readingScore;
    BigDecimal writingScore;
    BigDecimal speakingScore;
    PlacementLevel recommendedLevel;
    Set<AssessmentSkill> weakSkills;
    String targetExam;
    BigDecimal targetScore;
}
