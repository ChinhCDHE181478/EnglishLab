package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ContentReviewRequest {
    @Size(max = 1000)
    private String reviewNote;
}
