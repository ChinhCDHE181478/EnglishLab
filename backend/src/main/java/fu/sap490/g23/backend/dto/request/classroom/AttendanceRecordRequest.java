package fu.sap490.g23.backend.dto.request.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomAttendanceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttendanceRecordRequest {

    @NotNull(message = "Học viên không được để trống")
    private Long studentId;

    @NotNull(message = "Trạng thái điểm danh không được để trống")
    private ClassroomAttendanceStatus status;

    private String note;
    private LocalDateTime joinTime;
    private LocalDateTime leaveTime;
    private Integer durationMinutes;
}
