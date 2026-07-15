package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CompletePracticeRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomPracticeResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomPracticeAttemptResponse;

import java.util.List;

public interface ClassroomPracticeService {
    List<ClassroomPracticeResponse> listForLearner(Long offeringId, String learnerEmail);
    List<ClassroomPracticeResponse> listAllForLearner(String learnerEmail);
    ClassroomPracticeResponse complete(Long offeringId, Long exerciseId, CompletePracticeRequest request, String learnerEmail);
    ClassroomPracticeAttemptResponse submitAttempt(Long offeringId, Long exerciseId, CompletePracticeRequest request, String learnerEmail);
    List<ClassroomPracticeAttemptResponse> listAttempts(Long offeringId, Long exerciseId, String learnerEmail);
}
