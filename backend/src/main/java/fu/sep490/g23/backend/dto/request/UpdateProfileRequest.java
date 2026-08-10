package fu.sap490.g23.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateProfileRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Phone number is required")
    private String phoneNumber;

    @NotBlank(message = "Target exam is required")
    private String targetExam;

    private String targetScore;
    private Double currentBand;
    private String studyGoal;
}
