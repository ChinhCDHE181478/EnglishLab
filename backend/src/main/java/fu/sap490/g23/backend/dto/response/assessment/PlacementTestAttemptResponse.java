package fu.sap490.g23.backend.dto.response.assessment;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlacementTestAttemptResponse {
    private Long id;
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
    private LocalDateTime submittedAt;
}
