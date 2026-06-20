package fu.sap490.g23.backend.dto.response.assessment;

import fu.sap490.g23.backend.entity.assessment.*;
import fu.sap490.g23.backend.entity.assessment.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAssessmentResponse {
    private Long id;
    private Long courseId;
    private Long moduleId;
    private String moduleTitle;
    private String title;
    private String description;
    private AssessmentType type;
    private AssessmentSkill skill;
    private AiEvaluationMode aiEvaluationMode;
    private String instructions;
    private String objectiveAnswerKey;
    private String uiConfigJson;
    private BigDecimal passingScore;
    private BigDecimal maxScore;
    private Integer timeLimitMinutes;
    private Integer displayOrder;
    private boolean active;
    private AssessmentRubricResponse rubric;
    private AiAssessmentSubmissionResponse latestSubmission;
    private AiAssessmentSubmissionResponse previousSubmission;
}
