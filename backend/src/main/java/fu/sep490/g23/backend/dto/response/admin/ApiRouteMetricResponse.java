package fu.sep490.g23.backend.dto.response.admin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiRouteMetricResponse {
    private String method;
    private String route;
    private long requestCount;
    private long errorCount;
    private double errorRatePercent;
    private double averageLatencyMs;
    private long maximumLatencyMs;
}
