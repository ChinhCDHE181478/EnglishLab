package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomAttendanceStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class ClassroomAttendanceResponse {
    private Long id;
    private Long sessionId;
    private Long studentId;
    private String studentName;
    private String studentEmail;
    private ClassroomAttendanceStatus status;
    private String note;
    private LocalDateTime joinTime;
    private LocalDateTime leaveTime;
    private Integer durationMinutes;
    private boolean teacherConfirmed;

    // Session metadata (populated by getBySession so the frontend has context)
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private String classroomTitle;
    private Long classSectionId;
    private String deliveryMode;
    private String roomName;
    private String googleMeetUrl;
    private String googleMeetSyncError;
}
