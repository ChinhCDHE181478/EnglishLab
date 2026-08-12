package fu.sep490.g23.backend.dto.request.curriculum;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CurriculumUnitRequest {
    @Min(0)
    private Integer displayOrder;

    @NotBlank(message = "Tên unit/buổi học không được để trống.")
    @Size(max = 180)
    private String title;

    @Size(max = 700)
    private String description;

    private String sessionPlan;
}
