package fu.sep490.g23.backend.dto.request.curriculum;

import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AssessmentBankItemRequest {
    @NotBlank(message = "Tên đề không được để trống.")
    @Size(max = 180)
    private String title;

    @Size(max = 700)
    private String description;

    @NotNull(message = "Loại đề không được để trống.")
    private AssessmentType type;

    @NotNull(message = "Kỹ năng không được để trống.")
    private AssessmentSkill skill;

    private AiEvaluationMode aiEvaluationMode;
    private Long rubricId;
    private String instructions;
    private String objectiveAnswerKey;
    private String uiConfigJson;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal passingScore;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "100.0")
    private BigDecimal maxScore;

    @Min(0)
    private Integer timeLimitMinutes;

    @Size(max = 30)
    private String status;

    @Min(0)
    private Integer displayOrder;
}
