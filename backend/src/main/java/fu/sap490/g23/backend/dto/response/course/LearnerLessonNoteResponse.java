package fu.sap490.g23.backend.dto.response.course;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LearnerLessonNoteResponse {
    private Long id;
    private Long courseId;
    private Long lessonId;
    private String lessonTitle;
    private String courseTitle;
    private String content;
    private String selectedText;
    private Integer transcriptStartSeconds;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
