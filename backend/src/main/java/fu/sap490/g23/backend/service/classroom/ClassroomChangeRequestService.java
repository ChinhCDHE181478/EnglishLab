package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CreateChangeRequestRequest;
import fu.sap490.g23.backend.dto.request.classroom.ReviewChangeRequestRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomChangeRequestResponse;

import java.util.List;

public interface ClassroomChangeRequestService {

    ClassroomChangeRequestResponse create(CreateChangeRequestRequest request, String requesterEmail);

    List<ClassroomChangeRequestResponse> listMine(String requesterEmail);

    List<ClassroomChangeRequestResponse> listPending();

    ClassroomChangeRequestResponse approve(Long requestId, ReviewChangeRequestRequest request, String reviewerEmail);

    ClassroomChangeRequestResponse reject(Long requestId, ReviewChangeRequestRequest request, String reviewerEmail);
}
