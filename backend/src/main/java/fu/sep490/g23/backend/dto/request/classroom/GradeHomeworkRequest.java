package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.DecimalMin;
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
public class GradeHomeworkRequest {

    @NotNull(message = "Vui lòng nhập điểm")
    @DecimalMin(value = "0", message = "Điểm không được âm")
    private BigDecimal score;

    @Size(max = 5000, message = "Nhận xét không được vượt quá 5.000 ký tự")
    private String teacherFeedback;

    @Valid
    @Size(max = 100, message = "Mỗi bài làm chỉ được có tối đa 100 ghi chú")
    private List<HomeworkTextAnnotationRequest> annotations;
}
