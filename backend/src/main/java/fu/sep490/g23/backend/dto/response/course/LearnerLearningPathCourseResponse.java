package fu.sep490.g23.backend.dto.response.course;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LearnerLearningPathCourseResponse {
    private Long courseId;
    private String slug;
    private String title;
    private String thumbnailUrl;
    private Integer learningPathOrder;
    private String enrollmentStatus;
    private Integer progressPercent;
    private boolean completed;
    private String lockedReason;
    private String stepStatus;
}
