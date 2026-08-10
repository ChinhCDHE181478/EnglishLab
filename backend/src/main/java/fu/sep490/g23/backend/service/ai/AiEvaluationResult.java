package fu.sap490.g23.backend.service.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiEvaluationResult {
    private BigDecimal estimatedScore;
    private String feedbackJson;
    private String provider;
    private String model;
    private String rawResponse;
    private boolean audioInputAnalyzed;
}
