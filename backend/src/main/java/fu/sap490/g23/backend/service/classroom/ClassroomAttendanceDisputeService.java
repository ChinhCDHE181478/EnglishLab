package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CreateAttendanceDisputeRequest;
import fu.sap490.g23.backend.dto.request.classroom.ReviewAttendanceDisputeRequest;
import fu.sap490.g23.backend.dto.response.classroom.AttendanceDisputeResponse;

import java.util.List;

public interface ClassroomAttendanceDisputeService {
    AttendanceDisputeResponse create(Long attendanceId, CreateAttendanceDisputeRequest request, String studentEmail);
    List<AttendanceDisputeResponse> listForStudent(String studentEmail);
    List<AttendanceDisputeResponse> listForClass(Long offeringId);
    List<AttendanceDisputeResponse> listPending();
    AttendanceDisputeResponse review(Long disputeId, ReviewAttendanceDisputeRequest request, String reviewerEmail);
}
