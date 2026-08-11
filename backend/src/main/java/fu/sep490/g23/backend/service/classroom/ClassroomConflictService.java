package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;

public interface ClassroomConflictService {

    ConflictCheckResultResponse check(ConflictCheckRequest request);
    void assertNoBlockingConflict(ConflictCheckRequest request);
}
