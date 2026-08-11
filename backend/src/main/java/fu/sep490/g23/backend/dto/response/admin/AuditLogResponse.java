package fu.sep490.g23.backend.dto.response.admin;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @Builder
public class AuditLogResponse {
    private Long id; private String actorEmail; private String action; private String targetType; private String targetId; private String detail; private LocalDateTime createdAt;
}
