package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ClassroomConflictErrorResponse {
    private int status;
    private String message;
    private LocalDateTime timestamp;
    private ConflictCheckResultResponse conflicts;
}
