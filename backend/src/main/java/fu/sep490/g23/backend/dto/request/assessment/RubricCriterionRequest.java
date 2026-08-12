package fu.sep490.g23.backend.dto.request.assessment;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RubricCriterionRequest {
    private Long id;

    @NotBlank(message = "Tên tiêu chí không được để trống.")
    @Size(max = 120, message = "Tên tiêu chí không được vượt quá 120 ký tự.")
    private String name;

    @Min(value = 0, message = "Trọng số không được âm.")
    @Max(value = 100, message = "Trọng số không được vượt quá 100.")
    private Integer weight = 25;

    @Size(max = 500, message = "Mô tả tiêu chí không được vượt quá 500 ký tự.")
    private String description;

    private String bandDescriptors;

    @Min(value = 0, message = "Thứ tự hiển thị không được âm.")
    private Integer displayOrder = 0;
}
