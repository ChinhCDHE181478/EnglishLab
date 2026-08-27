package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.request.assessment.ReviewPlacementAttemptRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementEligibilityResult;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;

import java.util.List;

/** Decides whether a scored attempt can be used for course placement / recommendations. */
public interface PlacementEligibilityService {
    /** Check completeness, expiry, fraud, and whether staff review is still needed. */
    PlacementEligibilityResult evaluateEligibility(Long learnerId, Long placementAttemptId);

    /** Attempts that still need a training-staff confirm (IELTS Writing/Speaking). */
    List<PlacementTestAttemptResponse> listManualReviewQueue(String staffEmail);

    /** Staff accept the attempt and assign a recommended level. */
    PlacementEligibilityResult confirmManualReview(
            Long placementAttemptId,
            ReviewPlacementAttemptRequest request,
            String reviewerEmail
    );
}
