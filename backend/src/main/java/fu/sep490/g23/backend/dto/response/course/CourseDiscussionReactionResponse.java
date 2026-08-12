package fu.sep490.g23.backend.dto.response.course;

import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReactionType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CourseDiscussionReactionResponse {
    private Long userId;
    private String userName;
    private CourseDiscussionReactionType type;
    private LocalDateTime createdAt;
}
