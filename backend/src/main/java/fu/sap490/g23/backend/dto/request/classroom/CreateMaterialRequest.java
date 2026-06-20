package fu.sap490.g23.backend.dto.request.classroom;

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
public class CreateMaterialRequest {

    @NotBlank(message = "Tiêu đề tài liệu không được để trống")
    @Size(max = 220)
    private String title;

    @Size(max = 700)
    private String fileUrl;

    @Size(max = 80)
    private String fileType;

    private String visibility;
    private Long sessionId;
}
