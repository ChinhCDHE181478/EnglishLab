package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.request.assessment.MockTestSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.MockTestAttemptResponse;

public interface MockTestService {
    MockTestAttemptResponse submitMockTest(Long mockTestId, MockTestSubmissionRequest request, String studentEmail);
}
