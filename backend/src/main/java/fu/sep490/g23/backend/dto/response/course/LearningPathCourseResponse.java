package fu.sep490.g23.backend.dto.response.course;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LearningPathCourseResponse {
    private Long courseId;
    private String slug;
    private String title;
    private String thumbnailUrl;
    private String targetOutcome;
    private Integer displayOrder;
}
