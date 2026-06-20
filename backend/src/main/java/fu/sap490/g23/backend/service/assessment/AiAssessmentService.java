package fu.sap490.g23.backend.service.assessment;

import fu.sap490.g23.backend.dto.request.assessment.AssessmentSubmissionRequest;
import fu.sap490.g23.backend.dto.response.assessment.AiAssessmentSubmissionResponse;
import fu.sap490.g23.backend.dto.response.assessment.CourseAssessmentResponse;

import java.util.List;

public interface AiAssessmentService {
    List<CourseAssessmentResponse> getCourseAssessments(Long courseId, String studentEmail);
    AiAssessmentSubmissionResponse submitAssessment(Long assessmentId, AssessmentSubmissionRequest request, String studentEmail);
}
