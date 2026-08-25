package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.request.assessment.PlacementTestSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;
import java.util.Map;

/** Student-facing placement test: load the paper and score a submission. */
public interface PlacementTestService {

    /** Build the current paper payload (no answer keys) for the given student. */
    Map<String, Object> getTest(String studentEmail);

    /** Validate, score, and save one attempt. */
    PlacementTestAttemptResponse submit(PlacementTestSubmissionRequest request, String studentEmail);
}
