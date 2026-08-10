package fu.sap490.g23.backend.dto.request.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.AttendanceDisputeStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewAttendanceDisputeRequest {
    @NotNull(message = "Trạng thái xử lý không được để trống.")
    private AttendanceDisputeStatus status;
    private String reviewNote;
    private String attendanceStatus;
}
