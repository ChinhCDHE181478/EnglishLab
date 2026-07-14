package fu.sap490.g23.backend.dto.response.course;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LearnerLessonReviewFlagResponse {
    private Long id;
    private Long courseId;
    private Long lessonId;
    private String lessonTitle;
    private String courseTitle;
    private LocalDateTime createdAt;
}
