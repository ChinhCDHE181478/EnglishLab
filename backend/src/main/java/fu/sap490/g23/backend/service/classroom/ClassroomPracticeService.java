package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CompletePracticeRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomPracticeResponse;

import java.util.List;

public interface ClassroomPracticeService {
    List<ClassroomPracticeResponse> listForLearner(Long offeringId, String learnerEmail);
    ClassroomPracticeResponse complete(Long offeringId, Long exerciseId, CompletePracticeRequest request, String learnerEmail);
}
