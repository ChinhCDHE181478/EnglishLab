package fu.sep490.g23.backend.dto.request.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseDiscussionThreadRequest {
    @NotBlank(message = "Vui lòng nhập tiêu đề câu hỏi.")
    @Size(min = 8, max = 180, message = "Tiêu đề cần từ 8 đến 180 ký tự.")
    private String title;

    @NotBlank(message = "Vui lòng nhập nội dung câu hỏi.")
    @Size(min = 20, max = 3000, message = "Câu hỏi cần rõ ràng hơn để mọi người có thể hỗ trợ bạn.")
    private String content;
}
