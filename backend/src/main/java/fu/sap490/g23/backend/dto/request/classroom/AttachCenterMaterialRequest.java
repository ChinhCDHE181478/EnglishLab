package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AttachCenterMaterialRequest {

    @NotNull(message = "Vui lòng chọn học liệu từ thư viện trung tâm.")
    private Long centerMaterialId;

    private Long sessionId;
}
