package fu.sep490.g23.backend.dto.request.course;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseReviewRequest {
    @NotNull(message = "Vui lòng chọn số sao đánh giá.") @Min(value = 1, message = "Đánh giá phải từ 1 đến 5 sao.") @Max(value = 5, message = "Đánh giá phải từ 1 đến 5 sao.")
    private Integer rating;
    @Size(max = 2000, message = "Nhận xét không được vượt quá 2000 ký tự.") private String comment;
}
