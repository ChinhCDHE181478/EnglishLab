package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.ContentReviewStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PendingContentReviewResponse {
    private String contentType;
    private Long id;
    private Long classroomOfferingId;
    private String classroomTitle;
    private String title;
    private ContentReviewStatus reviewStatus;
    private String reviewNote;
    private LocalDateTime submittedForReviewAt;
    private LocalDateTime updatedAt;
}
