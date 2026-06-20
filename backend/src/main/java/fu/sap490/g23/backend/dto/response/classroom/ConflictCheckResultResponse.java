package fu.sap490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class ConflictCheckResultResponse {
    @Builder.Default
    private boolean hasBlockingConflict = false;
    @Builder.Default
    private boolean canOverride = false;
    @Builder.Default
    private List<ConflictItemResponse> conflicts = new ArrayList<>();
}
