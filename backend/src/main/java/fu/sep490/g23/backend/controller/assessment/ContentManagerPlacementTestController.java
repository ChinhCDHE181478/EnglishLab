package fu.sep490.g23.backend.controller.assessment;

import fu.sep490.g23.backend.dto.request.assessment.PlacementTestDefinitionRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestDefinitionResponse;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestMonitoringResponse;
import fu.sep490.g23.backend.service.assessment.PlacementTestDefinitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/content-manager/placement-test")
@RequiredArgsConstructor
public class ContentManagerPlacementTestController {
    private final PlacementTestDefinitionService definitionService;

    @GetMapping
    public ResponseEntity<PlacementTestDefinitionResponse> getDefinition() {
        return ResponseEntity.ok(definitionService.getManagementDefinition());
    }

    @PutMapping
    public ResponseEntity<PlacementTestDefinitionResponse> updateDefinition(
            @Valid @RequestBody PlacementTestDefinitionRequest request
    ) {
        return ResponseEntity.ok(definitionService.updateDefinition(request));
    }

    @GetMapping("/monitoring")
    public ResponseEntity<PlacementTestMonitoringResponse> getMonitoring() {
        return ResponseEntity.ok(definitionService.getMonitoring());
    }
}
