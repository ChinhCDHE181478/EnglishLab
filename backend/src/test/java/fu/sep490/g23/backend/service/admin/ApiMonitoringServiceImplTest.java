package fu.sep490.g23.backend.service.admin;

import fu.sep490.g23.backend.dto.response.admin.ApiMonitoringResponse;
import fu.sep490.g23.backend.service.admin.impl.ApiMonitoringServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApiMonitoringServiceImplTest {
    @Mock private JdbcTemplate jdbcTemplate;

    @Test
    void summary_NormalizesIdsAndCalculatesErrorRate() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class)).thenReturn(1);
        ApiMonitoringServiceImpl service = new ApiMonitoringServiceImpl(jdbcTemplate);
        service.record("GET", "/api/courses/17", 200, 20, "request-ok");
        service.record("GET", "/api/courses/18", 500, 40, "request-failed");

        ApiMonitoringResponse response = service.getSummary();

        assertEquals("UP", response.getApplicationStatus());
        assertEquals(2, response.getTotalRequests());
        assertEquals(1, response.getTotalErrors());
        assertEquals(50.0, response.getErrorRatePercent());
        assertEquals("/api/courses/{id}", response.getBusiestRoutes().getFirst().getRoute());
        assertEquals(2, response.getBusiestRoutes().getFirst().getRequestCount());
        assertEquals("request-failed", response.getRecentFailures().getFirst().getCorrelationId());
    }

    @Test
    void summary_WhenDatabaseFails_ReportsDegradedWithoutThrowing() {
        when(jdbcTemplate.queryForObject("SELECT 1", Integer.class))
                .thenThrow(new RuntimeException("connection refused"));
        ApiMonitoringServiceImpl service = new ApiMonitoringServiceImpl(jdbcTemplate);

        ApiMonitoringResponse response = service.getSummary();

        assertEquals("DEGRADED", response.getApplicationStatus());
        assertEquals("DOWN", response.getDatabaseStatus());
    }
}
