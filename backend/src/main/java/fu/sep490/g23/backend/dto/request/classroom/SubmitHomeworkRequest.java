package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitHomeworkRequest {

    @Size(max = 100000, message = "Nội dung bài nộp không được vượt quá 100.000 ký tự")
    private String textAnswer;

    @Size(max = 700, message = "Đường dẫn tệp đính kèm không được vượt quá 700 ký tự")
    private String attachmentUrl;
}
