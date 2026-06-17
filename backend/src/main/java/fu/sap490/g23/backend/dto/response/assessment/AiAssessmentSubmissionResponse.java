package fu.sap490.g23.backend.dto.response.assessment;

import fu.sap490.g23.backend.entity.assessment.SubmissionStatus;
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
    private String aiPromptSnapshot;
    private String submittedText;
    private String submittedAudioUrl;
    private String objectiveAnswersJson;
    private String aiProvider;
    private String aiModel;
    private SubmissionStatus status;
    private LocalDateTime submittedAt;
}
