package fu.sep490.g23.backend.controller.assessment;

import fu.sep490.g23.backend.dto.request.assessment.ReviewPlacementAttemptRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementEligibilityResult;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;
import fu.sep490.g23.backend.service.assessment.PlacementEligibilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Staff APIs to confirm IELTS Writing/Speaking after auto-score.
 * Confirmation sets ELIGIBLE + recommendedLevel so course recommendations can fully unlock.
 */
@RestController
@RequestMapping("/api/staff/placement-reviews")
@RequiredArgsConstructor
public class StaffPlacementReviewController {

    private final PlacementEligibilityService placementEligibilityService;

    /** List attempts waiting for (or currently in) manual review. */
    @GetMapping
    public ResponseEntity<List<PlacementTestAttemptResponse>> listManualReviewQueue(Authentication authentication) {
        return ResponseEntity.ok(placementEligibilityService.listManualReviewQueue(authentication.getName()));
    }

    /** Staff confirms the attempt: mark eligible and store the placement level. */
    @PatchMapping("/{attemptId}/review")
    public ResponseEntity<PlacementEligibilityResult> confirmManualReview(
            @PathVariable Long attemptId,
            @Valid @RequestBody ReviewPlacementAttemptRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(placementEligibilityService.confirmManualReview(
                attemptId,
                request,
                authentication.getName()
        ));
    }
}
