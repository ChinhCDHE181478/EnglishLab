package fu.sep490.g23.backend.dto.request.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.GradebookEntryStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateGradebookRequest {

    @NotNull(message = "Học viên không được để trống")
    private Long studentId;

    @Valid
    private List<UpdateGradebookHomeworkScoreRequest> homeworkScores;

    @DecimalMin(value = "0.0", message = "Chuyên cần không được nhỏ hơn 0%")
    @DecimalMax(value = "100.0", message = "Chuyên cần không được lớn hơn 100%")
    @Digits(integer = 3, fraction = 2, message = "Chuyên cần chỉ được có tối đa 2 chữ số thập phân")
    private BigDecimal attendancePercent;

    @DecimalMin(value = "0.0", message = "Kết quả không được nhỏ hơn 0")
    @DecimalMax(value = "10.0", message = "Kết quả không được lớn hơn 10")
    @Digits(integer = 2, fraction = 2, message = "Kết quả chỉ được có tối đa 2 chữ số thập phân")
    private BigDecimal finalResult;

    @Size(max = 2000, message = "Nhận xét của giáo viên không được vượt quá 2000 ký tự")
    private String teacherComment;
    private GradebookEntryStatus status;
}
