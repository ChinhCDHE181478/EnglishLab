package fu.sep490.g23.backend.dto.response.curriculum;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CourseUnitResponse {
    private Long id;
    private Long programId;
    private Integer displayOrder;
    private String title;
    private String description;
    private String sessionPlan;
    private List<CourseLessonResponse> lessons;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CourseUnitContentRefResponse> materials;
    private List<CourseUnitContentRefResponse> exercises;
    private List<CourseUnitContentRefResponse> assessments;
    private List<CourseUnitContentRefResponse> flashcards;
}
