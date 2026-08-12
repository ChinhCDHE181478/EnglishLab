package fu.sep490.g23.backend.dto.request.assessment;

import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewPlacementAttemptRequest {
    @NotNull(message = "Trình độ đề xuất không được để trống")
    private PlacementLevel recommendedLevel;

    @NotBlank(message = "Ghi chú đánh giá thủ công không được để trống")
    @Size(max = 700)
    private String note;
}
