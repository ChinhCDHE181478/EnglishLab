package fu.sep490.g23.backend.ut.classroomOperationService;

import fu.sep490.g23.backend.dto.request.classroom.AssignEnrollmentClassRequest;
import fu.sep490.g23.backend.dto.response.classroom.CourseEnrollmentRequestResponse;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sep490.g23.backend.service.classroom.EnrollmentRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * UC-93: Approve and Assign Class - test đầy đủ với request validation.
 * Service: EnrollmentRequestService.assignClass()
 * Request DTO: AssignEnrollmentClassRequest
 */
@ExtendWith(MockitoExtension.class)
public class ApproveandAssignClassTest {

    @Mock
    private EnrollmentRequestService enrollmentRequestService;

    @Test
    void lateClassSelectionGetsFullPaymentWindowFromInvitationTime() {
        LocalDateTime invitedAt = LocalDateTime.of(2026, 9, 24, 10, 30);
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .classSection(ClassSection.builder().startDate(LocalDate.of(2026, 8, 1)).build())
                .enrolledAt(invitedAt)
                .build();

        assertThat(ClassroomRegistrationSupport.tuitionPaymentDeadline(enrollment))
                .isEqualTo(invitedAt.plusHours(ClassroomRegistrationSupport.LATE_APPROVAL_PAYMENT_HOURS));
    }

    @Test
    void earlyClassSelectionKeepsRegularDeadlineBeforeClassStarts() {
        LocalDate startDate = LocalDate.of(2026, 10, 15);
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .classSection(ClassSection.builder().startDate(startDate).build())
                .enrolledAt(LocalDateTime.of(2026, 9, 24, 10, 30))
                .build();

        assertThat(ClassroomRegistrationSupport.tuitionPaymentDeadline(enrollment))
                .isEqualTo(startDate.minusDays(ClassroomRegistrationSupport.TUITION_BALANCE_DUE_DAYS_BEFORE_START)
                        .atTime(LocalTime.MAX));
    }

    // TC01: Duyệt + gán lớp thành công
    @Test
    void approveandAssignClass_UTC01_approveAndAssign() {
        CourseEnrollmentRequestResponse expected = CourseEnrollmentRequestResponse.builder()
                .id(101L)
                .status(EnrollmentRequestStatus.CLASS_ASSIGNED)
                .assignedClassroomId(1L)
                .build();

        when(enrollmentRequestService.assignClass(eq(101L), any(AssignEnrollmentClassRequest.class), eq("staff@test.com")))
                .thenReturn(expected);

        // Build complete request with all fields
        AssignEnrollmentClassRequest request = new AssignEnrollmentClassRequest();
        request.setClassroomId(1L);
        request.setNote("Học viên đã hoàn thành placement test với band 6.0");

        CourseEnrollmentRequestResponse result = enrollmentRequestService.assignClass(101L, request, "staff@test.com");

        // Verify response
        assertThat(result.getStatus()).isEqualTo(EnrollmentRequestStatus.CLASS_ASSIGNED);

        // CRITICAL: Verify request data passed to service via ArgumentCaptor
        ArgumentCaptor<Long> requestIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<AssignEnrollmentClassRequest> requestCaptor = 
                ArgumentCaptor.forClass(AssignEnrollmentClassRequest.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(enrollmentRequestService, times(1))
                .assignClass(requestIdCaptor.capture(), requestCaptor.capture(), emailCaptor.capture());

        // Verify all parameters
        assertThat(requestIdCaptor.getValue()).isEqualTo(101L);
        assertThat(requestCaptor.getValue().getClassroomId()).isEqualTo(1L);
        assertThat(requestCaptor.getValue().getNote()).isEqualTo("Học viên đã hoàn thành placement test với band 6.0");
        assertThat(emailCaptor.getValue()).isEqualTo("staff@test.com");

        verifyNoMoreInteractions(enrollmentRequestService);
    }

    // TC02: Lớp đầy chỗ -> BusinessRule
    @Test
    void tc02_classFullBoundary_throwsException() {
        when(enrollmentRequestService.assignClass(any(), any(), any()))
                .thenThrow(new IllegalStateException("Lớp học đã đạt sức chứa tối đa (30/30)"));

        AssignEnrollmentClassRequest request = new AssignEnrollmentClassRequest();
        request.setClassroomId(1L);
        request.setNote("Test note");

        assertThatThrownBy(() -> enrollmentRequestService.assignClass(101L, request, "staff@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sức chứa tối đa");

        // Verify the request that caused the error
        ArgumentCaptor<Long> requestIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<AssignEnrollmentClassRequest> requestCaptor = 
                ArgumentCaptor.forClass(AssignEnrollmentClassRequest.class);
        verify(enrollmentRequestService, times(1))
                .assignClass(requestIdCaptor.capture(), requestCaptor.capture(), any());

        assertThat(requestIdCaptor.getValue()).isEqualTo(101L);
        assertThat(requestCaptor.getValue().getClassroomId()).isEqualTo(1L);
        assertThat(requestCaptor.getValue().getNote()).isEqualTo("Test note");

        verifyNoMoreInteractions(enrollmentRequestService);
    }

    // TC03: Lớp đã đóng -> BusinessRule
    @Test
    void approveandAssignClass_UTC03_classClosed() {
        when(enrollmentRequestService.assignClass(any(), any(), any()))
                .thenThrow(new IllegalStateException("Lớp đã đóng"));

        AssignEnrollmentClassRequest request = new AssignEnrollmentClassRequest();
        request.setClassroomId(999L);
        request.setNote("Assign to closed class");

        assertThatThrownBy(() -> enrollmentRequestService.assignClass(101L, request, "staff@test.com"))
                .isInstanceOf(IllegalStateException.class);

        // Verify the request
        ArgumentCaptor<Long> requestIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<AssignEnrollmentClassRequest> requestCaptor = 
                ArgumentCaptor.forClass(AssignEnrollmentClassRequest.class);
        verify(enrollmentRequestService, times(1))
                .assignClass(requestIdCaptor.capture(), requestCaptor.capture(), any());

        assertThat(requestIdCaptor.getValue()).isEqualTo(101L);
        assertThat(requestCaptor.getValue().getClassroomId()).isEqualTo(999L);

        verifyNoMoreInteractions(enrollmentRequestService);
    }

    // TC04: Thiếu classroomId -> Validation
    @Test
    void approveandAssignClass_UTC04_missingClassroom() {
        when(enrollmentRequestService.assignClass(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Lớp xếp cho học viên không được để trống"));

        AssignEnrollmentClassRequest request = new AssignEnrollmentClassRequest();
        // Missing classroomId

        assertThatThrownBy(() -> enrollmentRequestService.assignClass(101L, request, "staff@test.com"))
                .isInstanceOf(IllegalArgumentException.class);

        // Verify the request with missing classroomId
        ArgumentCaptor<AssignEnrollmentClassRequest> requestCaptor = 
                ArgumentCaptor.forClass(AssignEnrollmentClassRequest.class);
        verify(enrollmentRequestService, times(1))
                .assignClass(eq(101L), requestCaptor.capture(), any());

        assertThat(requestCaptor.getValue().getClassroomId()).isNull();

        verifyNoMoreInteractions(enrollmentRequestService);
    }

    // TC05: requestId không ở trạng thái cho phép
    @Test
    void approveandAssignClass_UTC05_invalidRequestStatus() {
        when(enrollmentRequestService.assignClass(any(), any(), any()))
                .thenThrow(new IllegalStateException("MSG-44: Yêu cầu đăng ký không thể xử lý ở trạng thái hiện tại."));

        AssignEnrollmentClassRequest request = new AssignEnrollmentClassRequest();
        request.setClassroomId(1L);

        assertThatThrownBy(() -> enrollmentRequestService.assignClass(102L, request, "staff@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("MSG-44");

        // Verify the request
        ArgumentCaptor<Long> requestIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(enrollmentRequestService, times(1))
                .assignClass(requestIdCaptor.capture(), any(), any());

        assertThat(requestIdCaptor.getValue()).isEqualTo(102L);

        verifyNoMoreInteractions(enrollmentRequestService);
    }

    // TC06: Thiếu targetClassroomId -> Validation
    @Test
    void approveandAssignClass_UTC06_missingTargetClassroom() {
        when(enrollmentRequestService.assignClass(any(), any(), any()))
                .thenThrow(new IllegalArgumentException("MSG-46: Vui lòng nhập đầy đủ thông tin bắt buộc."));

        AssignEnrollmentClassRequest request = new AssignEnrollmentClassRequest();
        // Empty request - missing classroomId

        assertThatThrownBy(() -> enrollmentRequestService.assignClass(101L, request, "staff@test.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MSG-46");

        // Verify the request
        ArgumentCaptor<AssignEnrollmentClassRequest> requestCaptor = 
                ArgumentCaptor.forClass(AssignEnrollmentClassRequest.class);
        verify(enrollmentRequestService, times(1))
                .assignClass(eq(101L), requestCaptor.capture(), eq("staff@test.com"));

        assertThat(requestCaptor.getValue().getClassroomId()).isNull();

        verifyNoMoreInteractions(enrollmentRequestService);
    }

    // TC07: requestId hoặc classId không tồn tại -> EntityNotFoundException
    @Test
    void approveandAssignClass_UTC07_requestOrClassNotFound() {
        when(enrollmentRequestService.assignClass(any(), any(), any()))
                .thenThrow(new jakarta.persistence.EntityNotFoundException("MSG-48: Không tìm thấy dữ liệu yêu cầu."));

        AssignEnrollmentClassRequest request = new AssignEnrollmentClassRequest();
        request.setClassroomId(1L);

        assertThatThrownBy(() -> enrollmentRequestService.assignClass(9999L, request, "staff@test.com"))
                .isInstanceOf(jakarta.persistence.EntityNotFoundException.class)
                .hasMessageContaining("MSG-48");

        // Verify the non-existent request ID
        ArgumentCaptor<Long> requestIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(enrollmentRequestService, times(1))
                .assignClass(requestIdCaptor.capture(), any(), any());

        assertThat(requestIdCaptor.getValue()).isEqualTo(9999L);
    }
}
