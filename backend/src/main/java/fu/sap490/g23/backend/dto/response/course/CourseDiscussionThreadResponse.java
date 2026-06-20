package fu.sap490.g23.backend.dto.response.course;

import fu.sap490.g23.backend.entity.course.CourseDiscussionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class CourseDiscussionThreadResponse {
    private Long id;
    private String title;
    private String content;
    private CourseDiscussionStatus status;
    private int replyCount;
    private int helpfulCount;
    private Map<String, Integer> reactionCounts;
    private String myReaction;
    private int reportedCount;
    private boolean resolved;
    private String authorName;
    private Long authorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CourseDiscussionReplyResponse> replies;
}
