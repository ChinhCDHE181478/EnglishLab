package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.request.assessment.PlacementTestSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;
import java.util.Map;

public interface PlacementTestService {

    Map<String, Object> getTest(String studentEmail);

    PlacementTestAttemptResponse submit(PlacementTestSubmissionRequest request, String studentEmail);
}
