package fu.sep490.g23.backend.controller.classroom;

import fu.sep490.g23.backend.dto.response.classroom.StaffDashboardResponse;
import fu.sep490.g23.backend.service.classroom.StaffOperationsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/staff")
@RequiredArgsConstructor
public class StaffDashboardController {

    private final StaffOperationsService staffOperationsService;

    @GetMapping("/dashboard")
    public ResponseEntity<StaffDashboardResponse> dashboard() {
        return ResponseEntity.ok(staffOperationsService.getDashboard());
    }
}
