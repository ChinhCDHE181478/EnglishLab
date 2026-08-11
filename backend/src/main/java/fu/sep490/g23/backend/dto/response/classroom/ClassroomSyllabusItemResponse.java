package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomSyllabusItemResponse {
    private Long id;
    private String title;
    private String description;
    private Integer displayOrder;
    private String sessionPlan;
    private String homeworkNotes;
    private String quizNotes;
    private String teacherNotes;
    private Integer sessionNumber;
    private Long linkedSessionId;
    private String reviewStatus;
    private String reviewNote;
    private String status;
}
