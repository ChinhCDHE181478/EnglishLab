package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuizQuestionRequest {
    private Integer sortOrder;
    @NotBlank(message = "Nội dung câu hỏi không được để trống.")
    private String prompt;
    @NotBlank(message = "Danh sách đáp án không được để trống.")
    private String optionsJson;
    @NotBlank(message = "Đáp án đúng không được để trống.")
    private String correctAnswer;
    private String explanation;
}
