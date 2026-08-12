package fu.sep490.g23.backend.dto.request.assessment;

import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentManagerCourseAssessmentRequest {

    private Long id;
    private Long moduleId;
    private Long rubricId;
    private Long assessmentBankItemId;

    @NotBlank(message = "Assessment title is required")
    @Size(max = 180)
    private String title;

    @Size(max = 700)
    private String description;

    @NotNull(message = "Assessment type is required")
    private AssessmentType type;

    @NotNull(message = "Assessment skill is required")
    private AssessmentSkill skill;

    @NotNull(message = "AI evaluation mode is required")
    private AiEvaluationMode aiEvaluationMode;

    private String instructions;
    private String objectiveAnswerKey;
    private String uiConfigJson;

    @DecimalMin(value = "0.0")
    private BigDecimal passingScore;

    @DecimalMin(value = "0.0")
    private BigDecimal maxScore;

    @Min(0)
    private Integer timeLimitMinutes;

    @Min(0)
    private Integer displayOrder;

    private Boolean active;
}
