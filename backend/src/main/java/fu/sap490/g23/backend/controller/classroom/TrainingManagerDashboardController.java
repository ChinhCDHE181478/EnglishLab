package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.response.classroom.TrainingManagerDashboardResponse;
import fu.sap490.g23.backend.service.classroom.TrainingManagerOpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/training-manager")
@RequiredArgsConstructor
public class TrainingManagerDashboardController {

    private final TrainingManagerOpsService trainingManagerOpsService;

    @GetMapping("/dashboard")
    public ResponseEntity<TrainingManagerDashboardResponse> dashboard() {
        return ResponseEntity.ok(trainingManagerOpsService.getDashboard());
    }
}
