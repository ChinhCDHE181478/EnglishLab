package fu.sap490.g23.backend.dto.request.classroom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewChangeRequestRequest {

    private String reviewNote;
    private boolean overrideConflict;
}
