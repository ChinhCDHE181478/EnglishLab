package fu.sap490.g23.backend.service.admin;

import fu.sap490.g23.backend.dto.response.admin.ApiMonitoringResponse;

public interface ApiMonitoringService {
    void record(String method, String requestPath, int status, long durationMs, String correlationId);
    ApiMonitoringResponse getSummary();
}
