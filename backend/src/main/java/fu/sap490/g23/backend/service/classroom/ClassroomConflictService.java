package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sap490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;

public interface ClassroomConflictService {

    ConflictCheckResultResponse check(ConflictCheckRequest request);
    void assertNoBlockingConflict(ConflictCheckRequest request);
}
