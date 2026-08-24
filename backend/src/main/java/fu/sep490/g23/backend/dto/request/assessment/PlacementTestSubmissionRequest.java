package fu.sep490.g23.backend.dto.request.assessment;

import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class PlacementTestSubmissionRequest {
    @Size(max = 100, message = "Mã bài đánh giá không được vượt quá 100 ký tự")
    private String testCode;

    @Pattern(regexp = "(?i)IELTS|TOEIC|SKILL", message = "Loại bài đánh giá chỉ hỗ trợ IELTS, TOEIC hoặc đánh giá kỹ năng")
    private String examType;

    @Size(max = 4, message = "Chỉ có thể chọn tối đa 4 kỹ năng")
    private List<AssessmentSkill> selectedSkills;

    @Size(max = 500, message = "Phần Listening không được vượt quá 500 câu trả lời")
    private Map<String, Object> listeningAnswers;

    @Size(max = 500, message = "Phần Reading không được vượt quá 500 câu trả lời")
    private Map<String, Object> readingAnswers;

    @Size(max = 100, message = "Phần Writing không được vượt quá 100 mục trả lời")
    private Map<String, Object> writingAnswers;

    @Size(max = 100000, message = "Bản chép lời Speaking không được vượt quá 100.000 ký tự")
    private String speakingTranscript;

    @Size(max = 700, message = "Đường dẫn tệp Speaking không được vượt quá 700 ký tự")
    private String speakingAudioUrl;

    @Size(max = 50, message = "Dữ liệu kiểm tra thiết bị không hợp lệ")
    private Map<String, Object> deviceCheck;
}
