package fu.sep490.g23.backend.dto.response.assessment;

import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class PlacementEligibilityResult {
    private Long attemptId;
    private boolean eligible;
    private PlacementEvaluationStatus status;
    private List<String> missingRequirements;
    private PlacementLevel recommendedLevel;
    private LocalDateTime expiresAt;
    private Long reviewerId;
    private LocalDateTime reviewedAt;
    private String reviewNote;
}
