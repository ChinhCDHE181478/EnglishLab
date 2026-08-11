package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.CreateAttendanceDisputeRequest;
import fu.sep490.g23.backend.dto.request.classroom.ReviewAttendanceDisputeRequest;
import fu.sep490.g23.backend.dto.response.classroom.AttendanceDisputeResponse;

import java.util.List;

public interface ClassroomAttendanceDisputeService {
    AttendanceDisputeResponse create(Long attendanceId, CreateAttendanceDisputeRequest request, String studentEmail);
    List<AttendanceDisputeResponse> listForStudent(String studentEmail);
    List<AttendanceDisputeResponse> listForClass(Long offeringId, String teacherEmail);
    List<AttendanceDisputeResponse> listPending(String teacherEmail);
    AttendanceDisputeResponse review(Long disputeId, ReviewAttendanceDisputeRequest request, String reviewerEmail);
}
