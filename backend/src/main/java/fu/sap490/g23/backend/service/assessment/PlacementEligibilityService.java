package fu.sap490.g23.backend.service.assessment;

import fu.sap490.g23.backend.dto.request.assessment.ReviewPlacementAttemptRequest;
import fu.sap490.g23.backend.dto.response.assessment.PlacementEligibilityResult;
import fu.sap490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;

import java.util.List;

public interface PlacementEligibilityService {
    PlacementEligibilityResult evaluateEligibility(Long learnerId, Long placementAttemptId);

    List<PlacementTestAttemptResponse> listManualReviewQueue(String staffEmail);

    PlacementEligibilityResult confirmManualReview(
            Long placementAttemptId,
            ReviewPlacementAttemptRequest request,
            String reviewerEmail
    );
}
