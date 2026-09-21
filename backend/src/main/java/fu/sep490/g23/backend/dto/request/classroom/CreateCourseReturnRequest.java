package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateCourseReturnRequest {

    @NotNull(message = "Không tìm thấy yêu cầu bảo lưu cần kết thúc.")
    private Long suspensionRequestId;
}
