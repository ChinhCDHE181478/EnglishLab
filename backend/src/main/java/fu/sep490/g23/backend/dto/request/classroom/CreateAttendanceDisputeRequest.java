package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAttendanceDisputeRequest {
    @NotBlank(message = "Vui lòng mô tả lý do khiếu nại.")
    private String reason;
}
