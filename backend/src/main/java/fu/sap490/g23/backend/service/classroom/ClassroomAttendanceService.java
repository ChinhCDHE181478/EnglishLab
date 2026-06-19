package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.SaveAttendanceRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomAttendanceResponse;

import java.util.List;

public interface ClassroomAttendanceService {

    List<ClassroomAttendanceResponse> getBySession(Long sessionId);

    List<ClassroomAttendanceResponse> getByClass(Long offeringId);

    List<ClassroomAttendanceResponse> getByClassForStudent(Long offeringId, String learnerEmail);

    List<ClassroomAttendanceResponse> saveBulk(SaveAttendanceRequest request, String markerEmail);
}
