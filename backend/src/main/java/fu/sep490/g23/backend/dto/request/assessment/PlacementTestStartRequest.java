package fu.sep490.g23.backend.dto.request.assessment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PlacementTestStartRequest {
    @NotBlank(message = "Cần chọn dạng bài đánh giá")
    @Pattern(regexp = "(?i)IELTS|TOEIC|SKILL", message = "Loại bài đánh giá chỉ hỗ trợ IELTS, TOEIC hoặc đánh giá kỹ năng")
    private String examType;
}
