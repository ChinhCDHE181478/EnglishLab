package fu.sap490.g23.backend.dto.response.assessment;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
public class WritingFeedbackResponse {
    private BigDecimal estimatedScore;
    private String overallFeedback;
    private List<String> strengths;
    private List<String> improvements;
    private List<Criterion> criteria;
    private List<String> correctedHighlights;
    private boolean fallback;

    @Getter
    @Builder
    public static class Criterion {
        private String name;
        private BigDecimal score;
        private String feedback;
    }
}
