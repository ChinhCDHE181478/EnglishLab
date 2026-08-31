package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.UpdateRecordingRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;

import java.util.List;

public interface ClassroomRecordingService {
    ClassroomSessionResponse updateSessionRecording(Long sessionId, UpdateRecordingRequest request);

    List<ClassroomSessionResponse> listManagerSessions(Long offeringId);

    ClassroomSessionResponse syncRecording(Long sessionId);

}
