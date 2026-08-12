package fu.sep490.g23.backend.dto.response.teacher;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ManagerTeacherFeedbackDetailResponse {
    private TeacherFeedbackAggregateResponse aggregate;
    private List<AnonymizedTeacherFeedbackResponse> feedback;
}
