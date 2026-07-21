package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RejectEnrollmentRequest {
    @NotBlank(message = "Lý do từ chối không được để trống")
    @Size(max = 700)
    private String reason;
}
