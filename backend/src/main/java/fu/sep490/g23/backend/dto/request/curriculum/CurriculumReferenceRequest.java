package fu.sep490.g23.backend.dto.request.curriculum;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CurriculumReferenceRequest {
    @NotNull(message = "Tài nguyên cần gắn không được để trống.")
    private Long resourceId;

    @Min(0)
    private Integer displayOrder;

    @Size(max = 500)
    private String note;
}
