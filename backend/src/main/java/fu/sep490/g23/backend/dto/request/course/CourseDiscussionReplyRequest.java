package fu.sep490.g23.backend.dto.request.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseDiscussionReplyRequest {
    @NotBlank(message = "Vui lòng nhập nội dung trả lời.")
    @Size(min = 2, max = 3000, message = "Nội dung trả lời cần từ 2 đến 3000 ký tự.")
    private String content;
}
