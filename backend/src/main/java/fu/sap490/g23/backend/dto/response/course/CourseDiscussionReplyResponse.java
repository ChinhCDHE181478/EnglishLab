package fu.sap490.g23.backend.dto.response.course;

import fu.sap490.g23.backend.entity.course.CourseDiscussionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
public class CourseDiscussionReplyResponse {
    private Long id;
    private String content;
    private CourseDiscussionStatus status;
    private boolean accepted;
    private int helpfulCount;
    private Map<String, Integer> reactionCounts;
    private String myReaction;
    private String authorName;
    private Long authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
