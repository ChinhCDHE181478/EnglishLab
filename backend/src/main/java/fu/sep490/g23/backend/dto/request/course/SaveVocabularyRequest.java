package fu.sap490.g23.backend.dto.request.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SaveVocabularyRequest {

    @NotBlank(message = "Từ vựng không được để trống.")
    @Size(max = 120, message = "Từ vựng không được vượt quá 120 ký tự.")
    private String word;

    @Size(max = 180, message = "Phiên âm không được vượt quá 180 ký tự.")
    private String phonetic;

    @NotBlank(message = "Nghĩa chính không được để trống.")
    @Size(max = 1200, message = "Nghĩa chính không được vượt quá 1.200 ký tự.")
    private String primaryDefinition;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1.000 ký tự.")
    private String note;
}
