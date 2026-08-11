package fu.sep490.g23.backend.dto.request.assessment;

import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AssessmentSubmissionRequest {
    @Size(max = 100000, message = "Nội dung bài làm không được vượt quá 100.000 ký tự")
    private String submittedText;

    @Size(max = 700, message = "Đường dẫn tệp âm thanh không được vượt quá 700 ký tự")
    private String submittedAudioUrl;

    @Size(max = 250000, message = "Dữ liệu câu trả lời không được vượt quá 250.000 ký tự")
    private String objectiveAnswersJson;

    @PositiveOrZero(message = "Số lần thoát toàn màn hình không được âm")
    private Integer fullscreenExitCount;

    @PositiveOrZero(message = "Số lần chuyển tab không được âm")
    private Integer tabSwitchCount;

    private Boolean microphoneChecked;
    private Boolean deviceCheckPassed;
}
