package fu.sep490.g23.backend.servicev2.placementtest;

import fu.sep490.g23.backend.dto.request.assessment.PlacementTestSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;

public interface IPlacementTestService {
    PlacementTestAttemptResponse submit(PlacementTestSubmissionRequest request, String studentEmail);
}
