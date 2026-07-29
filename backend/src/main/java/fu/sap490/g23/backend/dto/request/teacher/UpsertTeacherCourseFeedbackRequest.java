package fu.sap490.g23.backend.dto.request.teacher;

import fu.sap490.g23.backend.entity.teacher.enums.TeacherFeedbackPace;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpsertTeacherCourseFeedbackRequest {
    @Min(value = 1, message = "Điểm mức độ dễ hiểu phải từ 1 đến 5.")
    @Max(value = 5, message = "Điểm mức độ dễ hiểu phải từ 1 đến 5.")
    private int clarityScore;

    @Min(value = 1, message = "Điểm mức độ cuốn hút phải từ 1 đến 5.")
    @Max(value = 5, message = "Điểm mức độ cuốn hút phải từ 1 đến 5.")
    private int engagementScore;

    @Min(value = 1, message = "Điểm hỗ trợ học viên phải từ 1 đến 5.")
    @Max(value = 5, message = "Điểm hỗ trợ học viên phải từ 1 đến 5.")
    private int learnerSupportScore;

    @Min(value = 1, message = "Điểm phản hồi bài học phải từ 1 đến 5.")
    @Max(value = 5, message = "Điểm phản hồi bài học phải từ 1 đến 5.")
    private int feedbackTimelinessScore;

    @Min(value = 1, message = "Điểm tác phong phải từ 1 đến 5.")
    @Max(value = 5, message = "Điểm tác phong phải từ 1 đến 5.")
    private int professionalismScore;

    @NotNull(message = "Vui lòng đánh giá tốc độ giảng dạy.")
    private TeacherFeedbackPace pace;

    @NotNull(message = "Vui lòng cho biết bạn có sẵn sàng giới thiệu giáo viên hay không.")
    private Boolean wouldRecommend;

    @NotBlank(message = "Vui lòng mô tả điểm mạnh của giáo viên.")
    @Size(min = 20, max = 1500, message = "Điểm mạnh cần từ 20 đến 1.500 ký tự.")
    private String strengths;

    @NotBlank(message = "Vui lòng đưa ra góp ý cải thiện cụ thể.")
    @Size(min = 20, max = 1500, message = "Góp ý cải thiện cần từ 20 đến 1.500 ký tự.")
    private String improvementSuggestions;

    @Size(max = 1500, message = "Nhận xét bổ sung không được vượt quá 1.500 ký tự.")
    private String additionalComment;
}
