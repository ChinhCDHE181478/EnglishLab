package fu.sap490.g23.backend.dto.response.course;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class LearningPathResponse {
    private Long id;
    private String code;
    private String name;
    private List<LearningPathCourseResponse> courses;
}
