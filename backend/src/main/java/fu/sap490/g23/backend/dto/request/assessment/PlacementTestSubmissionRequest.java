package fu.sap490.g23.backend.dto.request.assessment;

import lombok.Data;

import java.util.Map;

@Data
public class PlacementTestSubmissionRequest {
    private String testCode;
    private Map<String, Object> listeningAnswers;
    private Map<String, Object> readingAnswers;
    private Map<String, Object> writingAnswers;
    private String speakingTranscript;
    private String speakingAudioUrl;
    private Map<String, Object> deviceCheck;
}
