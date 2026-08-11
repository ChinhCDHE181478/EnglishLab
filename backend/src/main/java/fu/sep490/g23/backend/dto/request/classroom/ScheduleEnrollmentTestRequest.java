package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleEnrollmentTestRequest {
    @NotNull(message = "Ngày giờ đến test không được để trống")
    private LocalDateTime appointmentAt;

    @NotBlank(message = "Địa điểm test không được để trống")
    @Size(max = 300)
    private String location;

    @Size(max = 700)
    private String note;
}
