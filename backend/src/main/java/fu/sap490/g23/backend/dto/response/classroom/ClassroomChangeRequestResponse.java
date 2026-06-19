package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.*;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class ClassroomChangeRequestResponse {
    private Long id;
    private ClassroomChangeRequestType requestType;
    private String requestTypeLabel;
    private Long requesterId;
    private String requesterName;
    private Long classroomOfferingId;
    private String classroomTitle;
    private Long targetSessionId;
    private String oldValuesJson;
    private String newValuesJson;
    private String reason;
    private ClassroomChangeRequestStatus status;
    private String statusLabel;
    private Long reviewerId;
    private String reviewerName;
    private LocalDateTime reviewedAt;
    private String reviewNote;
    private LocalDateTime createdAt;
}
