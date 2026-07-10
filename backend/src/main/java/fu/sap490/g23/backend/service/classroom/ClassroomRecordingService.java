package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.UpdateRecordingRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomSessionResponse;

public interface ClassroomRecordingService {
    ClassroomOfferingResponse updateOfferingRecording(Long offeringId, UpdateRecordingRequest request);

    ClassroomSessionResponse updateSessionRecording(Long sessionId, UpdateRecordingRequest request);
}
