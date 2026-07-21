package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.ConfirmEnrollmentPlacementRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateCourseEnrollmentRequest;
import fu.sap490.g23.backend.dto.request.classroom.RejectEnrollmentRequest;
import fu.sap490.g23.backend.dto.request.classroom.CompleteEnrollmentConsultationRequest;
import fu.sap490.g23.backend.dto.request.classroom.AssignEnrollmentClassRequest;
import fu.sap490.g23.backend.dto.response.classroom.CourseEnrollmentRequestResponse;
import fu.sap490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;

import java.util.List;

public interface EnrollmentRequestService {
    CourseEnrollmentRequestResponse submit(CreateCourseEnrollmentRequest request, String learnerEmail);

    List<CourseEnrollmentRequestResponse> listMine(String learnerEmail);

    CourseEnrollmentRequestResponse refreshPlacement(Long requestId, String learnerEmail);

    CourseEnrollmentRequestResponse cancel(Long requestId, String learnerEmail);

    List<CourseEnrollmentRequestResponse> listForStaff(EnrollmentRequestStatus status, String staffEmail);

    CourseEnrollmentRequestResponse confirmPlacementLevel(
            Long requestId,
            ConfirmEnrollmentPlacementRequest request,
            String staffEmail
    );

    CourseEnrollmentRequestResponse reject(Long requestId, RejectEnrollmentRequest request, String staffEmail);

    CourseEnrollmentRequestResponse completeConsultation(
            Long requestId,
            CompleteEnrollmentConsultationRequest request,
            String staffEmail
    );

    CourseEnrollmentRequestResponse assignClass(
            Long requestId,
            AssignEnrollmentClassRequest request,
            String staffEmail
    );
}
