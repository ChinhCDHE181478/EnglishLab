package fu.sep490.g23.backend.dto.response.assessment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PlacementSkillScoresResponse {
    private BigDecimal listening;
    private BigDecimal reading;
    private BigDecimal writing;
    private BigDecimal speaking;
}
