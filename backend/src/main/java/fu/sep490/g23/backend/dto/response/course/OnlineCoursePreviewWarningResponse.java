package fu.sep490.g23.backend.dto.response.course;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OnlineCoursePreviewWarningResponse {
    private String code;
    private String severity;
    private String location;
    private String message;
}
