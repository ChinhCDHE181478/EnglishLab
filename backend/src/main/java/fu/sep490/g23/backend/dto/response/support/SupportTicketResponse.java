package fu.sep490.g23.backend.dto.response.support;

import fu.sep490.g23.backend.entity.support.enums.SupportTicketCategory;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketPriority;
import fu.sep490.g23.backend.entity.support.enums.SupportTicketStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class SupportTicketResponse {
    private Long id;
    private String subject;
    private SupportTicketCategory category;
    private SupportTicketStatus status;
    private SupportTicketPriority priority;
    private Long requesterId;
    private String requesterName;
    private String requesterEmail;
    private Long assigneeId;
    private String assigneeName;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<SupportTicketMessageResponse> messages;
}
