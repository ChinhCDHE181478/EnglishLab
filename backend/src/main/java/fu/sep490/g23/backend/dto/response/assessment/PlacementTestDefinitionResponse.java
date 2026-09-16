package fu.sep490.g23.backend.dto.response.assessment;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class PlacementTestDefinitionResponse {
    String testCode;
    String title;
    String description;
    String examType;
    Integer maxAttempts;
    String status;
    Boolean ieltsEnabled;
    Boolean toeicEnabled;
    Boolean skillAssessmentEnabled;
    String listeningConfigJson;
    String readingConfigJson;
    String writingConfigJson;
    String speakingConfigJson;
    String toeicConfigJson;
    LocalDateTime updatedAt;
}
