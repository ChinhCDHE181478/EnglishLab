package fu.sep490.g23.backend.dto.response.assessment;

import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementTestAttemptResponse {
    private Long id;
    private Long learnerId;
    private String learnerName;
    private String learnerEmail;
    private String testCode;
    private String examType;
    private BigDecimal listeningScore;
    private BigDecimal readingScore;
    private BigDecimal writingScore;
    private BigDecimal speakingScore;
    private BigDecimal overallScore;
    private Integer correctListening;
    private Integer correctReading;
    private String aiFeedbackJson;
    private String status;
    private PlacementEvaluationStatus evaluationStatus;
    private PlacementLevel recommendedLevel;
    private LocalDateTime expiresAt;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private LocalDateTime submittedAt;
}
