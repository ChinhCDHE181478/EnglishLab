package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomAttendanceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClassroomAttendanceResponse {
    private Long id;
    private Long sessionId;
    private Long studentId;
    private String studentName;
    private ClassroomAttendanceStatus status;
    private String note;
    private LocalDateTime joinTime;
    private LocalDateTime leaveTime;
    private Integer durationMinutes;
    private boolean teacherConfirmed;
}
