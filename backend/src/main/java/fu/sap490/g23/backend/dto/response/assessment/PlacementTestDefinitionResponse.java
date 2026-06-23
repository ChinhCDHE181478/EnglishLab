package fu.sap490.g23.backend.dto.response.assessment;

import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class PlacementTestDefinitionResponse {
    String testCode;
    String title;
    String description;
    Integer maxAttempts;
    boolean active;
    String listeningConfigJson;
    String readingConfigJson;
    String writingConfigJson;
    String speakingConfigJson;
    LocalDateTime updatedAt;
}
