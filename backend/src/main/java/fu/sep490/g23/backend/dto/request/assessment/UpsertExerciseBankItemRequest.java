package fu.sep490.g23.backend.dto.request.assessment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertExerciseBankItemRequest {
    @NotBlank(message = "Tiêu đề không được để trống.")
    @Size(max = 220)
    private String title;

    @NotBlank(message = "Kỹ năng không được để trống.")
    @Size(max = 30)
    private String skill;

    @Size(max = 60)
    private String level;

    @Size(max = 30)
    private String exerciseType;

    @NotBlank(message = "Đề bài không được để trống.")
    private String prompt;

    private String answerKey;
    private String explanation;

    @Size(max = 500)
    private String tags;

    private Boolean active;
}
