package fu.sap490.g23.backend.dto.request.teacher;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class UpsertTeacherEvaluationRequest {

    @NotNull(message = "Ngày bắt đầu kỳ đánh giá là bắt buộc.")
    private LocalDate periodStart;

    @NotNull(message = "Ngày kết thúc kỳ đánh giá là bắt buộc.")
    private LocalDate periodEnd;

    @NotNull(message = "Điểm chất lượng giảng dạy là bắt buộc.")
    @DecimalMin(value = "1.0", message = "Điểm phải từ 1 đến 5.")
    @DecimalMax(value = "5.0", message = "Điểm phải từ 1 đến 5.")
    private BigDecimal lessonDeliveryScore;

    @NotNull(message = "Điểm hỗ trợ học viên là bắt buộc.")
    @DecimalMin(value = "1.0", message = "Điểm phải từ 1 đến 5.")
    @DecimalMax(value = "5.0", message = "Điểm phải từ 1 đến 5.")
    private BigDecimal learnerSupportScore;

    @NotNull(message = "Điểm đúng hạn chấm bài là bắt buộc.")
    @DecimalMin(value = "1.0", message = "Điểm phải từ 1 đến 5.")
    @DecimalMax(value = "5.0", message = "Điểm phải từ 1 đến 5.")
    private BigDecimal gradingTimelinessScore;

    @NotNull(message = "Điểm tác phong chuyên nghiệp là bắt buộc.")
    @DecimalMin(value = "1.0", message = "Điểm phải từ 1 đến 5.")
    @DecimalMax(value = "5.0", message = "Điểm phải từ 1 đến 5.")
    private BigDecimal professionalismScore;

    @Size(max = 1500, message = "Nội dung điểm mạnh không được vượt quá 1.500 ký tự.")
    private String strengths;

    @Size(max = 1500, message = "Nội dung cần cải thiện không được vượt quá 1.500 ký tự.")
    private String improvementAreas;

    @Size(max = 1500, message = "Kế hoạch hành động không được vượt quá 1.500 ký tự.")
    private String actionPlan;
}
