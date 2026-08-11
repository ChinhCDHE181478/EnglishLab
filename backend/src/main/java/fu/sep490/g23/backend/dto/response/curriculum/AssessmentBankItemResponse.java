package fu.sep490.g23.backend.dto.response.curriculum;

import fu.sep490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AssessmentBankItemResponse {
    private Long id;
    private String title;
    private String description;
    private AssessmentType type;
    private AssessmentSkill skill;
    private AiEvaluationMode aiEvaluationMode;
    private AssessmentRubricResponse rubric;
    private String instructions;
    private String objectiveAnswerKey;
    private String uiConfigJson;
    private BigDecimal passingScore;
    private BigDecimal maxScore;
    private Integer timeLimitMinutes;
    private String status;
    private Integer displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
