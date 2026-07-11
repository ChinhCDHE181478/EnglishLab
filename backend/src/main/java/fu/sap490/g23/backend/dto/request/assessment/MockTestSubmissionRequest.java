package fu.sap490.g23.backend.dto.request.assessment;

import lombok.Data;

@Data
public class MockTestSubmissionRequest {
    private String objectiveAnswersJson;
    private String submittedText;
    private String submittedAudioUrl;
}
