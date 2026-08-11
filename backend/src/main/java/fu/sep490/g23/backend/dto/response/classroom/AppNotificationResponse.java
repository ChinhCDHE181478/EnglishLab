package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AppNotificationResponse {
    private Long id;
    private String type;
    private String title;
    private String body;
    private String actionPath;
    private boolean read;
    private LocalDateTime createdAt;
}
