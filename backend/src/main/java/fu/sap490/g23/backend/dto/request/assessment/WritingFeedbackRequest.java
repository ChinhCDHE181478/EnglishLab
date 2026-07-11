package fu.sap490.g23.backend.dto.request.assessment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WritingFeedbackRequest {
    @Size(max = 200, message = "Tiêu đề không được vượt quá 200 ký tự.")
    private String prompt;

    @NotBlank(message = "Vui lòng nhập bài viết cần nhận phản hồi.")
    @Size(min = 80, max = 3000, message = "Bài viết cần có từ 80 đến 3000 ký tự.")
    private String essayText;

    @Size(max = 30, message = "Kỳ thi mục tiêu không hợp lệ.")
    private String targetExam;

    private BigDecimal targetBand;
}
