package fu.sap490.g23.backend.dto.request.support;

import fu.sap490.g23.backend.entity.support.enums.SupportTicketPriority;
import fu.sap490.g23.backend.entity.support.enums.SupportTicketStatus;
import lombok.Data;

@Data
public class UpdateSupportTicketRequest {
    private SupportTicketStatus status;
    private SupportTicketPriority priority;
}
