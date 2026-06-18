package fu.sap490.g23.backend.controller.assessment;

import fu.sap490.g23.backend.dto.request.assessment.PlacementTestSubmissionRequest;
import fu.sap490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;
import fu.sap490.g23.backend.service.assessment.PlacementTestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/student/placement-tests")
@RequiredArgsConstructor
public class PlacementTestController {
    private final PlacementTestService placementTestService;

    @GetMapping("/mock-1")
    public ResponseEntity<Map<String, Object>> getMockOne(Authentication authentication) {
        return ResponseEntity.ok(placementTestService.getTest(authentication.getName()));
    }

    @PostMapping("/mock-1/submit")
    public ResponseEntity<PlacementTestAttemptResponse> submitMockOne(
            @RequestBody PlacementTestSubmissionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(placementTestService.submit(request, authentication.getName()));
    }
}
