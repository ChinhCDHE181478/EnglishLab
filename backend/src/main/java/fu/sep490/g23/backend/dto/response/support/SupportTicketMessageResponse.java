package fu.sap490.g23.backend.dto.response.support;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SupportTicketMessageResponse {
    private Long id;
    private Long authorId;
    private String authorName;
    private boolean staffMessage;
    private String body;
    private LocalDateTime createdAt;
}
