package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveAttendanceRequest {

    @NotNull(message = "Buổi học không được để trống")
    private Long sessionId;

    @Valid
    @NotEmpty(message = "Danh sách điểm danh không được để trống")
    @Builder.Default
    private List<AttendanceRecordRequest> records = new ArrayList<>();
}
