package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CompletePracticeRequest {
    @Size(max = 10000, message = "Ghi chú luyện tập không được vượt quá 10000 ký tự")
    private String responseText;
}
