package fu.sep490.g23.backend.dto.response.assessment;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentType;

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
    private Long lessonId;
    private Long assessmentBankItemId;
    private String moduleTitle;
    private String lessonTitle;
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
    /** Ngưỡng pass thực tế sau khi áp dụng fallback. */
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
