package fu.sep490.g23.backend.dto.response.course;

import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineCourseVersionResponse {
    private Long id;
    private Long courseId;
    private Integer versionNumber;
    private CourseVersionStatus status;
    private Integer totalRequiredLessons;
    private Integer totalRequiredAssessments;
    private String changeNote;
    private String createdByName;
    private String publishedByName;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private OnlineCourseResponse content;
}
