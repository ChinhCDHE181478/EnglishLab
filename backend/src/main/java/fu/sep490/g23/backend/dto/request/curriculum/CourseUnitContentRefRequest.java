package fu.sep490.g23.backend.dto.request.curriculum;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CourseUnitContentRefRequest {
    @NotNull(message = "Tài nguyên cần gắn không được để trống.")
    private Long resourceId;

    @Min(0)
    private Integer displayOrder;

}
