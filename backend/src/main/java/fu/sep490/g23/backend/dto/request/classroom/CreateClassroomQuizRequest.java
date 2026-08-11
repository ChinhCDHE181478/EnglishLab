package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class CreateClassroomQuizRequest {
    private Long sessionId;
    @NotBlank(message = "Tiêu đề bài kiểm tra không được để trống.")
    private String title;
    private String description;
    private Integer timeLimitMinutes;
    private Integer passingScore;
    private LocalDateTime dueAt;
    @NotEmpty(message = "Bài kiểm tra cần ít nhất một câu hỏi.")
    @Valid
    private List<QuizQuestionRequest> questions;
}
