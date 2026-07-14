package fu.sap490.g23.backend.dto.request.assessment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PlacementTestDefinitionRequest {
    @NotBlank
    private String title;

    private String description;

    private String examType;

    @Min(1)
    @Max(10)
    private Integer maxAttempts;

    private boolean active;

    @NotBlank
    private String listeningConfigJson;

    @NotBlank
    private String readingConfigJson;

    @NotBlank
    private String writingConfigJson;

    @NotBlank
    private String speakingConfigJson;

    private String toeicConfigJson;
}
