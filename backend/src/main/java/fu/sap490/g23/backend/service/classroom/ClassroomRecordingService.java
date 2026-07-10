package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.UpdateRecordingRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomSessionResponse;

import java.util.List;

public interface ClassroomRecordingService {
    ClassroomOfferingResponse updateOfferingRecording(Long offeringId, UpdateRecordingRequest request);

    ClassroomSessionResponse updateSessionRecording(Long sessionId, UpdateRecordingRequest request);

    List<ClassroomSessionResponse> listManagerSessions(Long offeringId);

    ClassroomSessionResponse syncLarkRecording(Long sessionId);
}
