package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RequestStaffOpenClassRequest {
    @NotNull(message = "Khóa học là bắt buộc")
    private Long courseOfferingId;

    @Size(max = 700)
    private String note;
}
