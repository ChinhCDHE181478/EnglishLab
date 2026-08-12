package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class EnrollmentRequestHistoryResponse {
    private Long id;
    private EnrollmentRequestStatus fromStatus;
    private EnrollmentRequestStatus toStatus;
    private String statusLabel;
    private Long actorId;
    private String actorName;
    private String reason;
    private LocalDateTime createdAt;
}
