package fu.sep490.g23.backend.dto.request.support;

import fu.sep490.g23.backend.entity.support.enums.SupportTicketPriority;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketStatus;
import lombok.Data;

@Data
public class UpdateSupportTicketRequest {
    private SupportTicketStatus status;
    private SupportTicketPriority priority;
}
