package fu.sap490.g23.backend.dto.request.course;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CourseDiscussionReportRequest {
    @Size(max = 500, message = "Lý do báo cáo không được vượt quá 500 ký tự.")
    private String reason;
}
