package fu.sap490.g23.backend.dto.request.course;

import fu.sap490.g23.backend.entity.course.enums.VocabularyMasteryStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSavedVocabularyRequest {

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1.000 ký tự.")
    private String note;

    @NotNull(message = "Trạng thái học từ là bắt buộc.")
    private VocabularyMasteryStatus status;
}
