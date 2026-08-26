package fu.sep490.g23.backend.dto.response.curriculum;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class CourseLessonResponse {
    private Long id;
    private Long unitId;
    private String unitTitle;
    private Long programId;
    private Integer sessionNumber;
    private Integer displayOrder;
    private String title;
    private String description;
    private String learningObjectives;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
