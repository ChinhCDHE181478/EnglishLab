package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CompleteEnrollmentTestRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateCourseEnrollmentRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateCenterEnrollmentRequest;
import fu.sap490.g23.backend.dto.request.classroom.RejectEnrollmentRequest;
import fu.sap490.g23.backend.dto.request.classroom.ScheduleEnrollmentTestRequest;
import fu.sap490.g23.backend.dto.request.classroom.AssignEnrollmentClassRequest;
import fu.sap490.g23.backend.dto.response.classroom.CourseEnrollmentRequestResponse;
import fu.sap490.g23.backend.dto.response.classroom.EnrollmentDemandReportResponse;
import fu.sap490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;

import java.util.List;

public interface EnrollmentRequestService {
    CourseEnrollmentRequestResponse submit(CreateCourseEnrollmentRequest request, String learnerEmail);

    CourseEnrollmentRequestResponse createAtCenter(CreateCenterEnrollmentRequest request, String staffEmail);

    List<CourseEnrollmentRequestResponse> listMine(String learnerEmail);

    List<CourseEnrollmentRequestResponse> listForStaff(EnrollmentRequestStatus status, String staffEmail);

    CourseEnrollmentRequestResponse scheduleTest(
            Long requestId,
            ScheduleEnrollmentTestRequest request,
            String staffEmail
    );

    CourseEnrollmentRequestResponse completeTest(
            Long requestId,
            CompleteEnrollmentTestRequest request,
            String staffEmail
    );

    CourseEnrollmentRequestResponse reject(Long requestId, RejectEnrollmentRequest request, String staffEmail);

    CourseEnrollmentRequestResponse assignClass(
            Long requestId,
            AssignEnrollmentClassRequest request,
            String staffEmail
    );

    List<Long> listAvailableClassroomIds(Long requestId, String staffEmail);

    List<EnrollmentDemandReportResponse> getDemandReport(String managerEmail);
}
