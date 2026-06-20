package fu.sap490.g23.backend.dto.request.assessment;

import lombok.Data;

@Data
public class AssessmentSubmissionRequest {
    private String submittedText;
    private String submittedAudioUrl;
    private String objectiveAnswersJson;
}
