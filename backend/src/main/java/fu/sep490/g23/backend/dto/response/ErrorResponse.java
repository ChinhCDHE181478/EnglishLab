package fu.sep490.g23.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ErrorResponse {

    private int status;
    private String message;
    private String code;
    private LocalDateTime timestamp;
}
