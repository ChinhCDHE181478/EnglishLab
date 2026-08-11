package fu.sep490.g23.backend.dto.request.support;

import fu.sep490.g23.backend.entity.support.enums.SupportTicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LearnerSupportTicketStatusRequest {
    @NotNull(message = "Trạng thái không được để trống.")
    private SupportTicketStatus status;
}
