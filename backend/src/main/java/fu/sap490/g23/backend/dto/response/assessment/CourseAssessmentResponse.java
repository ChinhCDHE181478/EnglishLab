package fu.sap490.g23.backend.dto.response.assessment;

import fu.sap490.g23.backend.entity.assessment.*;
import fu.sap490.g23.backend.entity.assessment.enums.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseAssessmentResponse {
    private Long id;
    private Long courseId;
    private Long moduleId;
    private Long assessmentBankItemId;
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
    /** Ngưỡng pass thực tế sau khi áp dụng fallback (CMS → band mục tiêu khóa − 0.5). */
    private BigDecimal resolvedPassingThreshold;
    /** Nhãn tiếng Việt giải thích nguồn ngưỡng pass. */
    private String passingThresholdLabel;
    private Integer timeLimitMinutes;
    private Integer displayOrder;
    private boolean active;
    private AssessmentRubricResponse rubric;
    private AiAssessmentSubmissionResponse latestSubmission;
    private AiAssessmentSubmissionResponse previousSubmission;
}
