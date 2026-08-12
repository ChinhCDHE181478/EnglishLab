package fu.sep490.g23.backend.dto.response.admin;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ApiMonitoringResponse {
    private LocalDateTime measuredAt;
    private String applicationStatus;
    private String databaseStatus;
    private long databaseLatencyMs;
    private long uptimeSeconds;
    private long totalRequests;
    private long totalErrors;
    private double errorRatePercent;
    private double averageLatencyMs;
    private long maximumLatencyMs;
    private long usedHeapBytes;
    private long maximumHeapBytes;
    private int availableProcessors;
    private List<ApiRouteMetricResponse> busiestRoutes;
    private List<ApiFailureResponse> recentFailures;
    private String measurementScope;
}
