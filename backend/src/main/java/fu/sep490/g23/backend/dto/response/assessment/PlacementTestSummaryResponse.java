package fu.sep490.g23.backend.dto.response.assessment;

import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class PlacementTestSummaryResponse {
    private Long attemptId;
    private String examType;
    private BigDecimal listeningScore;
    private BigDecimal readingScore;
    private BigDecimal writingScore;
    private BigDecimal speakingScore;
    private BigDecimal overallScore;
    private PlacementEvaluationStatus evaluationStatus;
    private PlacementLevel recommendedLevel;
    private LocalDateTime submittedAt;
    private LocalDateTime reviewedAt;
}
