package fu.sep490.g23.backend.dto.response.assessment;

import fu.sep490.g23.backend.entity.assessment.enums.SubmissionStatus;
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
public class AiAssessmentSubmissionResponse {
    private Long id;
    private Long assessmentId;
    private String assessmentTitle;
    private BigDecimal aiScore;
    private String aiFeedbackJson;
    private String submittedText;
    private String submittedAudioUrl;
    private String objectiveAnswersJson;
    private SubmissionStatus status;
    private LocalDateTime submittedAt;
}
