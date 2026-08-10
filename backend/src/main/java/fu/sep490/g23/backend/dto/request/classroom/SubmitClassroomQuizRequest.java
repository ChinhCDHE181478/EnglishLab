package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitClassroomQuizRequest {
    @NotBlank(message = "Câu trả lời không được để trống.")
    private String answersJson;
}
