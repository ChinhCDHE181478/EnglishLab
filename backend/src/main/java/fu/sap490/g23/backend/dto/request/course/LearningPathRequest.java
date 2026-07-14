package fu.sap490.g23.backend.dto.request.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LearningPathRequest {
    @NotBlank
    @Size(max = 80)
    private String code;

    @NotBlank
    @Size(max = 180)
    private String name;
}
