package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AssignEnrollmentClassRequest {
    @NotNull(message = "Lớp xếp cho học viên không được để trống")
    private Long classroomId;

    @Size(max = 700)
    private String note;
}
