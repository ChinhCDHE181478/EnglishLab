package fu.sep490.g23.backend.dto.response.admin;

import fu.sep490.g23.backend.entity.admin.enums.BroadcastStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminBroadcastResponse {
    private Long id;
    private String title;
    private String message;
    private String targetRole;
    private String actionPath;
    private boolean sendInApp;
    private boolean sendEmail;
    private BroadcastStatus status;
    private LocalDateTime scheduledAt;
    private LocalDateTime sentAt;
    private int recipientCount;
    private int inAppSuccessCount;
    private int emailQueuedCount;
    private String failureReason;
    private String createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
