package fu.sep490.g23.backend.controller.assessment;

import fu.sep490.g23.backend.dto.request.assessment.PlacementTestSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;
import fu.sep490.g23.backend.service.assessment.PlacementTestService;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationService;
import fu.sep490.g23.backend.dto.response.assessment.PlacementRecommendationResponse;
import jakarta.validation.Valid;
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
    private final PlacementRecommendationService placementRecommendationService;

    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrent(Authentication authentication) {
        return ResponseEntity.ok(placementTestService.getTest(authentication.getName()));
    }

    @PostMapping("/current/submit")
    public ResponseEntity<PlacementTestAttemptResponse> submitCurrent(
            @Valid @RequestBody PlacementTestSubmissionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(placementTestService.submit(request, authentication.getName()));
    }

    @GetMapping("/{attemptId}/recommendations")
    public ResponseEntity<PlacementRecommendationResponse> getRecommendations(
            @PathVariable Long attemptId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(placementRecommendationService.getRecommendations(attemptId, authentication.getName()));
    }
}
