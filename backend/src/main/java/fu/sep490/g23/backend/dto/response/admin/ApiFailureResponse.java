package fu.sep490.g23.backend.dto.response.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ApiFailureResponse {
    private LocalDateTime occurredAt;
    private String method;
    private String route;
    private int status;
    private long durationMs;
    private String correlationId;
}
