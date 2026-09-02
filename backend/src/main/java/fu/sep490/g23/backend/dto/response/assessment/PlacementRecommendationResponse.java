package fu.sep490.g23.backend.dto.response.assessment;

import fu.sep490.g23.backend.dto.response.course.LearnerLearningPathResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class PlacementRecommendationResponse {
    private Long attemptId;
    private boolean recommendationReady;
    private String message;
    private String examType;
    private PlacementEvaluationStatus evaluationStatus;
    private BigDecimal overallScore;
    private PlacementLevel recommendedLevel;
    private PlacementSkillScoresResponse skillScores;
    private List<String> weakSkills;
    private List<OnlineCourseResponse> recommendedOnlineCourses;
    private List<RecommendedInstructorLedCourseResponse> recommendedInstructorLedCourses;
    private LearnerLearningPathResponse.PathOverview recommendedLearningPath;
    private boolean targetMissing;
}
