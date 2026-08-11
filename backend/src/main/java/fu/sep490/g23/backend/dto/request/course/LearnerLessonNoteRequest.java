package fu.sep490.g23.backend.dto.request.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class LearnerLessonNoteRequest {
    @NotBlank(message = "Nội dung ghi chú không được để trống.")
    @Size(max = 2000, message = "Nội dung ghi chú không được vượt quá 2000 ký tự.")
    private String content;

    @Size(max = 2000, message = "Đoạn văn được chọn không được vượt quá 2000 ký tự.")
    private String selectedText;

    @Min(value = 0, message = "Thời điểm bản chép lời không hợp lệ.")
    private Integer transcriptStartSeconds;
}
