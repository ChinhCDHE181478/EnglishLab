package fu.sap490.g23.backend.dto.request.admin;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ScheduleAdminBroadcastRequest {
    @NotNull(message = "Vui lòng chọn thời gian gửi.")
    @Future(message = "Thời gian gửi phải ở tương lai.")
    private LocalDateTime scheduledAt;
}
