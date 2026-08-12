package fu.sep490.g23.backend.controller.admin;

import fu.sep490.g23.backend.dto.response.admin.ApiMonitoringResponse;
import fu.sep490.g23.backend.service.admin.ApiMonitoringService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/monitoring")
@RequiredArgsConstructor
public class AdminMonitoringController {
    private final ApiMonitoringService service;

    @GetMapping
    public ResponseEntity<ApiMonitoringResponse> summary() {
        return ResponseEntity.ok(service.getSummary());
    }
}
