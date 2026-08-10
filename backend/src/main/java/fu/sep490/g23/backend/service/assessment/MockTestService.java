package fu.sap490.g23.backend.service.assessment;

import fu.sap490.g23.backend.dto.request.assessment.MockTestSubmissionRequest;
import fu.sap490.g23.backend.dto.response.assessment.MockTestAttemptResponse;

public interface MockTestService {
    MockTestAttemptResponse submitMockTest(Long mockTestId, MockTestSubmissionRequest request, String studentEmail);
}
