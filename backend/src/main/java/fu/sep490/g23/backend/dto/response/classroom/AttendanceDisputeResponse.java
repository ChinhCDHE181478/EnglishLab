package fu.sap490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AttendanceDisputeResponse {
    private Long id;
    private Long attendanceId;
    private Long sessionId;
    private String sessionTitle;
    private Long studentId;
    private String studentName;
    private String currentAttendanceStatus;
    private String reason;
    private String status;
    private String reviewNote;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    private LocalDateTime createdAt;
}
