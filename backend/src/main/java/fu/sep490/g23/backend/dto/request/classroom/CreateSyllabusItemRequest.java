package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateSyllabusItemRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    @Size(max = 220)
    private String title;

    private String description;
    private Integer displayOrder;
    private String sessionPlan;
    private String homeworkNotes;
    private String quizNotes;
    private String teacherNotes;
    private Integer sessionNumber;
    private Long linkedSessionId;
    private String status;
}
