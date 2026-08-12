package fu.sep490.g23.backend.dto.request.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCategoryRequest {

    @Size(max = 40)
    private String code;

    @NotBlank(message = "Tên danh mục không được để trống")
    @Size(max = 120)
    private String name;

    @Size(max = 500)
    private String description;

    @Min(0)
    private Integer displayOrder;

    private Boolean active;
}
