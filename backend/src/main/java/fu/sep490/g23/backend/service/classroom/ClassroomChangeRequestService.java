package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.CreateChangeRequestRequest;
import fu.sep490.g23.backend.dto.request.classroom.ReviewChangeRequestRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomChangeRequestResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ClassroomChangeRequestService {

    ConflictCheckResultResponse checkConflict(CreateChangeRequestRequest request, String requesterEmail);

    ClassroomChangeRequestResponse create(CreateChangeRequestRequest request, String requesterEmail);

    List<ClassroomChangeRequestResponse> listMine(String requesterEmail);

    Page<ClassroomChangeRequestResponse> pageMine(String requesterEmail, String statusGroup, String keyword, Pageable pageable);

    Map<String, Long> getMyStats(String requesterEmail);

    List<ClassroomChangeRequestResponse> listPending();

    ClassroomChangeRequestResponse approve(Long requestId, ReviewChangeRequestRequest request, String reviewerEmail);

    ClassroomChangeRequestResponse reject(Long requestId, ReviewChangeRequestRequest request, String reviewerEmail);

    ConflictCheckResultResponse checkPendingConflict(Long requestId);
}
