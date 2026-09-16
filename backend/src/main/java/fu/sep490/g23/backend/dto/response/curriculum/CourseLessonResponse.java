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
    private Long instructorLedCourseId;
    /** Order of the lesson within its unit (1, 2, 3…). */
    private Integer sessionNumber;
    private Integer displayOrder;
    /** Display code like "1.1", "2.3" (unit.lesson). */
    private String lessonCode;
    private Integer plannedSessionCount;
    private String title;
    private String description;
    private String learningObjectives;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
