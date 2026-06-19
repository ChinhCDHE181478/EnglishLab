package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.UpdateGradebookRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomGradebookResponse;

import java.util.List;

public interface ClassroomGradebookService {

    List<ClassroomGradebookResponse> getClassGradebook(Long offeringId);

    ClassroomGradebookResponse getMyGradebook(Long offeringId, String learnerEmail);

    ClassroomGradebookResponse updateEntry(Long offeringId, UpdateGradebookRequest request, String updaterEmail);

    List<ClassroomGradebookResponse> publishGradebook(Long offeringId, String publisherEmail);
}
