package fu.sap490.g23.backend.dto.request.classroom;

import fu.sap490.g23.backend.entity.assessment.enums.PlacementLevel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfirmEnrollmentPlacementRequest {
    @NotNull(message = "Trình độ phân lớp không được để trống")
    private PlacementLevel placementLevel;

    @Size(max = 700)
    private String note;
}
