package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class StaffDashboardScoreItemResponse {
    private String name;
    private String subtitle;
    private BigDecimal score;
    private String href;
}
