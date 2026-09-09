package fu.sep490.g23.backend.dto.request.curriculum;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FlashcardSetRequest {
    @NotBlank(message = "Tên bộ flashcard không được để trống.")
    @Size(max = 180)
    private String title;

    @Size(max = 700)
    private String description;

    @Size(max = 30)
    private String examCategory;

    @Size(max = 60)
    private String skill;

    @Size(max = 500)
    private String tags;

    private String cardsJson;

    @Size(max = 30)
    private String status;

}
