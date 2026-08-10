package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateGradebookHomeworkScoreRequest {

    @NotNull(message = "Bài tập không được để trống")
    private Long homeworkId;

    @DecimalMin(value = "0.0", message = "Điểm bài tập không được nhỏ hơn 0")
    @Digits(integer = 4, fraction = 2, message = "Điểm bài tập chỉ được có tối đa 2 chữ số thập phân")
    private BigDecimal score;
}
