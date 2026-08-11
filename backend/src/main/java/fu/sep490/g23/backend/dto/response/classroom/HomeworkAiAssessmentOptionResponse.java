package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class HomeworkAiAssessmentOptionResponse {
    private Long id;
    private String title;
    private String description;
    private AssessmentSkill skill;
    private String instructions;
    private String uiConfigJson;
    private BigDecimal maxScore;
    private Integer timeLimitMinutes;
    private Long rubricId;
    private String rubricName;
}
