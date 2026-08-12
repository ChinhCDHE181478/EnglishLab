package fu.sep490.g23.backend.dto.request.course;

import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportReasonCategory;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseDiscussionReportRequest {
    @NotNull(message = "Vui lòng chọn loại báo cáo.")
    private CourseDiscussionReportReasonCategory reasonCategory;

    @Size(max = 500, message = "Lý do báo cáo không được vượt quá 500 ký tự.")
    private String reason;
}
