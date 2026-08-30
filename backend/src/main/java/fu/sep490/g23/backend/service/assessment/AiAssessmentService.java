package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.request.assessment.AssessmentSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.AiAssessmentSubmissionResponse;
import fu.sep490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import java.util.List;

public interface AiAssessmentService {
 
    AiAssessmentSubmissionResponse submitAssessment(Long assessmentId, AssessmentSubmissionRequest request, String studentEmail);
}
