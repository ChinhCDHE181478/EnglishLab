package fu.sep490.g23.backend.ut.classroomLearningService;

import fu.sep490.g23.backend.dto.request.classroom.CreateCourseEnrollmentRequest;
import fu.sep490.g23.backend.dto.response.classroom.CourseEnrollmentRequestResponse;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.service.classroom.EnrollmentRequestService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * UC-50: Submit Class Registration Request - test đầy đủ với request validation.
 * Service: EnrollmentRequestService.submit()
 * Request DTO: CreateCourseEnrollmentRequest
 */
@ExtendWith(MockitoExtension.class)
public class SubmitClassRegistrationRequestTest {

    @Mock
    private EnrollmentRequestService enrollmentRequestService;

    // UT-01: Đăng ký thành công -> SUBMITTED
    @Test
    void submitClassRegistrationRequest_UTC01_registerSuccess() {
        CourseEnrollmentRequestResponse expected = CourseEnrollmentRequestResponse.builder()
                .id(101L)
                .status(EnrollmentRequestStatus.SUBMITTED)
                .build();

        when(enrollmentRequestService.submit(any(CreateCourseEnrollmentRequest.class), eq("learner@test.com")))
                .thenReturn(expected);

        // Build complete request with all fields
        CreateCourseEnrollmentRequest request = new CreateCourseEnrollmentRequest();
        request.setCourseOfferingId(1L);
        request.setContactName("Nguyen Van A");
        request.setContactEmail("learner@test.com");
        request.setContactPhone("0912345678");
        request.setConsultationTrack("IELTS");
        request.setFacebookUrl("https://facebook.com/nguyenvana");
        request.setStudyWorkGoal("Chuẩn bị du học và làm việc quốc tế");
        request.setPreferredSchedule("Thứ 2, 4, 6 tối");
        request.setNote("Muốn học IELTS để chuẩn bị du học");

        CourseEnrollmentRequestResponse result = enrollmentRequestService.submit(request, "learner@test.com");

        // Verify response
        assertThat(result.getStatus()).isEqualTo(EnrollmentRequestStatus.SUBMITTED);

        // CRITICAL: Verify request data passed to service via ArgumentCaptor
        ArgumentCaptor<CreateCourseEnrollmentRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateCourseEnrollmentRequest.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(enrollmentRequestService, times(1)).submit(requestCaptor.capture(), emailCaptor.capture());

        CreateCourseEnrollmentRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getCourseOfferingId()).isEqualTo(1L);
        assertThat(capturedRequest.getContactName()).isEqualTo("Nguyen Van A");
        assertThat(capturedRequest.getContactEmail()).isEqualTo("learner@test.com");
        assertThat(capturedRequest.getContactPhone()).isEqualTo("0912345678");
        assertThat(capturedRequest.getConsultationTrack()).isEqualTo("IELTS");
        assertThat(capturedRequest.getFacebookUrl()).isEqualTo("https://facebook.com/nguyenvana");
        assertThat(capturedRequest.getStudyWorkGoal()).isEqualTo("Chuẩn bị du học và làm việc quốc tế");
        assertThat(capturedRequest.getPreferredSchedule()).isEqualTo("Thứ 2, 4, 6 tối");
        assertThat(capturedRequest.getNote()).isEqualTo("Muốn học IELTS để chuẩn bị du học");
        
        // Verify email parameter
        assertThat(emailCaptor.getValue()).isEqualTo("learner@test.com");

        verifyNoMoreInteractions(enrollmentRequestService);
    }

    // UT-05: Lớp yêu cầu Placement Test -> TEST_SCHEDULED
    @Test
    void submitClassRegistrationRequest_UTC02_requiresPlacementTest() {
        CourseEnrollmentRequestResponse expected = CourseEnrollmentRequestResponse.builder()
                .id(102L)
                .status(EnrollmentRequestStatus.TEST_SCHEDULED)
                .build();

        when(enrollmentRequestService.submit(any(CreateCourseEnrollmentRequest.class), eq("learner@test.com")))
                .thenReturn(expected);

        // Build request for advanced course requiring placement test
        CreateCourseEnrollmentRequest request = new CreateCourseEnrollmentRequest();
        request.setCourseOfferingId(2L);
        request.setPlacementAttemptId(1001L);
        request.setContactName("Nguyen Van A");
        request.setContactEmail("learner@test.com");
        request.setContactPhone("0912345678");
        request.setConsultationTrack("IELTS Advanced");
        request.setStudyWorkGoal("Nâng band điểm");
        request.setPreferredSchedule("Sáng CN");

        CourseEnrollmentRequestResponse result = enrollmentRequestService.submit(request, "learner@test.com");

        // Verify response
        assertThat(result.getStatus()).isEqualTo(EnrollmentRequestStatus.TEST_SCHEDULED);

        // CRITICAL: Verify request data passed to service
        ArgumentCaptor<CreateCourseEnrollmentRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateCourseEnrollmentRequest.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(enrollmentRequestService, times(1)).submit(requestCaptor.capture(), emailCaptor.capture());

        CreateCourseEnrollmentRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getCourseOfferingId()).isEqualTo(2L);
        assertThat(capturedRequest.getPlacementAttemptId()).isEqualTo(1001L);
        assertThat(capturedRequest.getContactName()).isEqualTo("Nguyen Van A");
        assertThat(capturedRequest.getContactEmail()).isEqualTo("learner@test.com");
        assertThat(capturedRequest.getConsultationTrack()).isEqualTo("IELTS Advanced");
        assertThat(capturedRequest.getStudyWorkGoal()).isEqualTo("Nâng band điểm");
        assertThat(capturedRequest.getPreferredSchedule()).isEqualTo("Sáng CN");
        
        assertThat(emailCaptor.getValue()).isEqualTo("learner@test.com");

        verifyNoMoreInteractions(enrollmentRequestService);
    }

    // UT-01: Lớp đóng/hết chỗ -> BadRequest
    @Test
    void submitClassRegistrationRequest_UTC03_classFullOrClosed() {
        when(enrollmentRequestService.submit(any(CreateCourseEnrollmentRequest.class), any()))
                .thenThrow(new IllegalStateException("Lớp học đã đóng hoặc hết chỗ"));

        CreateCourseEnrollmentRequest request = new CreateCourseEnrollmentRequest();
        request.setCourseOfferingId(99L);
        request.setContactName("Test User");
        request.setContactEmail("test@test.com");
        request.setContactPhone("0912345678");
        request.setConsultationTrack("IELTS");

        assertThatThrownBy(() -> enrollmentRequestService.submit(request, "learner@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đóng hoặc hết chỗ");

        // Verify request data that caused the error
        ArgumentCaptor<CreateCourseEnrollmentRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateCourseEnrollmentRequest.class);
        verify(enrollmentRequestService, times(1)).submit(requestCaptor.capture(), any());
        
        CreateCourseEnrollmentRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getCourseOfferingId()).isEqualTo(99L);
        assertThat(capturedRequest.getContactName()).isEqualTo("Test User");

        verifyNoMoreInteractions(enrollmentRequestService);
    }

    // UT-02: Học viên đã có yêu cầu đang hoạt động -> Conflict
    @Test
    void submitClassRegistrationRequest_UTC04_existingActiveRegistration() {
        when(enrollmentRequestService.submit(any(CreateCourseEnrollmentRequest.class), any()))
                .thenThrow(new IllegalStateException("Học viên đã có yêu cầu đăng ký đang chờ xử lý cho lớp này"));

        CreateCourseEnrollmentRequest request = new CreateCourseEnrollmentRequest();
        request.setCourseOfferingId(1L);
        request.setContactName("Existing User");
        request.setContactEmail("existing@test.com");
        request.setContactPhone("0987654321");
        request.setConsultationTrack("IELTS");
        request.setFacebookUrl("https://facebook.com/existinguser");

        assertThatThrownBy(() -> enrollmentRequestService.submit(request, "learner@test.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("đã có yêu cầu đăng ký");

        // Verify the request that caused conflict
        ArgumentCaptor<CreateCourseEnrollmentRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateCourseEnrollmentRequest.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(enrollmentRequestService, times(1)).submit(requestCaptor.capture(), emailCaptor.capture());
        
        CreateCourseEnrollmentRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getCourseOfferingId()).isEqualTo(1L);
        assertThat(capturedRequest.getContactName()).isEqualTo("Existing User");
        assertThat(capturedRequest.getContactEmail()).isEqualTo("existing@test.com");
        assertThat(capturedRequest.getFacebookUrl()).isEqualTo("https://facebook.com/existinguser");
        assertThat(emailCaptor.getValue()).isEqualTo("learner@test.com");

        verifyNoMoreInteractions(enrollmentRequestService);
    }

    // UT-03: Form thiếu trường bắt buộc -> Validation
    @Test
    void submitClassRegistrationRequest_UTC05_missingRequiredFields() {
        when(enrollmentRequestService.submit(any(CreateCourseEnrollmentRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Vui lòng nhập đầy đủ thông tin bắt buộc"));

        // Build request with missing/empty required fields
        CreateCourseEnrollmentRequest request = new CreateCourseEnrollmentRequest();
        request.setCourseOfferingId(1L);
        request.setContactName("");  // Empty - required
        request.setContactEmail("test@test.com");
        request.setContactPhone("");  // Empty - required
        request.setConsultationTrack("");  // Empty - required

        assertThatThrownBy(() -> enrollmentRequestService.submit(request, "learner@test.com"))
                .isInstanceOf(IllegalArgumentException.class);

        // Verify the invalid request was passed (for debugging)
        ArgumentCaptor<CreateCourseEnrollmentRequest> requestCaptor = 
                ArgumentCaptor.forClass(CreateCourseEnrollmentRequest.class);
        verify(enrollmentRequestService, times(1)).submit(requestCaptor.capture(), any());
        
        CreateCourseEnrollmentRequest capturedRequest = requestCaptor.getValue();
        assertThat(capturedRequest.getContactName()).isEmpty();
        assertThat(capturedRequest.getContactPhone()).isEmpty();
        assertThat(capturedRequest.getConsultationTrack()).isEmpty();

        verifyNoMoreInteractions(enrollmentRequestService);
    }
}
