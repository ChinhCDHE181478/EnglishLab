package fu.sep490.g23.backend.dto.response.assessment;

import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MockTestAttemptResponse {
    private Long id;
    private Long mockTestId;
    private String mockTestTitle;
    private AssessmentSkill skill;
    private Integer correctCount;
    private Integer totalQuestions;
    private BigDecimal score;
    private BigDecimal percent;
    private String aiFeedbackJson;
    private String status;
    private String submittedText;
    private String submittedAudioUrl;
    private String objectiveAnswersJson;
    private LocalDateTime submittedAt;
}
