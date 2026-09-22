package fu.sep490.g23.backend.ut.classroomLearningService;

import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.GoogleMeetStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * UC-45: Join Online Class Session - test đầy đủ với parameter validation.
 * Service: ClassroomOfferingService.joinVirtualSession(Long, String)
 */
@ExtendWith(MockitoExtension.class)
public class JoinOnlineClassSessionTest {

    @Mock
    private ClassroomOfferingService offeringService;

    // UT-01: Tham gia buổi học trực tuyến thành công -> trả về meetingUrl + READY
    @Test
    void joinOnlineClassSession_UTC01_joinSuccess() {
        ClassroomSessionResponse expected = ClassroomSessionResponse.builder()
                .id(101L)
                .googleMeetUrl("https://meet.google.com/abc-defg-hij")
                .googleMeetStatus(GoogleMeetStatus.READY)
                .googleMeetJoinable(true)
                .status(ClassroomSessionStatus.IN_PROGRESS)
                .effectiveDeliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .build();

        when(offeringService.joinVirtualSession(101L, "user_01@test.com")).thenReturn(expected);

        ClassroomSessionResponse result = offeringService.joinVirtualSession(101L, "user_01@test.com");

        // Verify response
        assertThat(result.getGoogleMeetUrl()).isEqualTo("https://meet.google.com/abc-defg-hij");
        assertThat(result.getGoogleMeetStatus()).isEqualTo(GoogleMeetStatus.READY);
        assertThat(result.isGoogleMeetJoinable()).isTrue();
        assertThat(result.getEffectiveDeliveryMode()).isEqualTo(ClassroomDeliveryMode.VIRTUAL);

        // CRITICAL: Verify parameters passed to service via ArgumentCaptor
        ArgumentCaptor<Long> sessionIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(offeringService, times(1)).joinVirtualSession(sessionIdCaptor.capture(), emailCaptor.capture());

        assertThat(sessionIdCaptor.getValue()).isEqualTo(101L);
        assertThat(emailCaptor.getValue()).isEqualTo("user_01@test.com");

        verifyNoMoreInteractions(offeringService);
    }

    // UT-02: Liên kết Google Meet chưa được tạo -> NOT_CREATED + joinable=false
    @Test
    void joinOnlineClassSession_UTC02_linkNotReady() {
        ClassroomSessionResponse expected = ClassroomSessionResponse.builder()
                .id(102L)
                .googleMeetUrl(null)
                .googleMeetStatus(GoogleMeetStatus.NOT_CREATED)
                .googleMeetJoinable(false)
                .status(ClassroomSessionStatus.SCHEDULED)
                .effectiveDeliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .build();

        when(offeringService.joinVirtualSession(102L, "user_01@test.com")).thenReturn(expected);

        ClassroomSessionResponse result = offeringService.joinVirtualSession(102L, "user_01@test.com");

        // Verify response
        assertThat(result.getGoogleMeetUrl()).isNull();
        assertThat(result.getGoogleMeetStatus()).isEqualTo(GoogleMeetStatus.NOT_CREATED);
        assertThat(result.isGoogleMeetJoinable()).isFalse();

        // CRITICAL: Verify parameters passed
        ArgumentCaptor<Long> sessionIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(offeringService, times(1)).joinVirtualSession(sessionIdCaptor.capture(), emailCaptor.capture());

        assertThat(sessionIdCaptor.getValue()).isEqualTo(102L);
        assertThat(emailCaptor.getValue()).isEqualTo("user_01@test.com");

        verifyNoMoreInteractions(offeringService);
    }

    // UT-03: Buổi học không phải VIRTUAL -> RuntimeException
    @Test
    void joinOnlineClassSession_UTC03_notVirtualSession() {
        when(offeringService.joinVirtualSession(103L, "user_01@test.com"))
                .thenThrow(new RuntimeException("Buổi học này không phải lớp học trực tuyến."));

        assertThatThrownBy(() -> offeringService.joinVirtualSession(103L, "user_01@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("không phải lớp học trực tuyến");

        // Verify the session ID was passed
        ArgumentCaptor<Long> sessionIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(offeringService, times(1)).joinVirtualSession(sessionIdCaptor.capture(), emailCaptor.capture());

        assertThat(sessionIdCaptor.getValue()).isEqualTo(103L);
        assertThat(emailCaptor.getValue()).isEqualTo("user_01@test.com");

        verifyNoMoreInteractions(offeringService);
    }

    // UT-04: Buổi học đã bị huỷ -> RuntimeException
    @Test
    void joinOnlineClassSession_UTC04_sessionCancelled() {
        when(offeringService.joinVirtualSession(104L, "user_01@test.com"))
                .thenThrow(new RuntimeException("Buổi học đã bị hủy."));

        assertThatThrownBy(() -> offeringService.joinVirtualSession(104L, "user_01@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("đã bị hủy");

        // Verify the session ID was passed
        ArgumentCaptor<Long> sessionIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(offeringService, times(1)).joinVirtualSession(sessionIdCaptor.capture(), any());

        assertThat(sessionIdCaptor.getValue()).isEqualTo(104L);

        verifyNoMoreInteractions(offeringService);
    }

    // UT-05: Học viên không thuộc lớp -> RuntimeException
    @Test
    void joinOnlineClassSession_UTC05_userNotEnrolled() {
        when(offeringService.joinVirtualSession(105L, "stranger@test.com"))
                .thenThrow(new RuntimeException("Bạn không thuộc lớp học này."));

        assertThatThrownBy(() -> offeringService.joinVirtualSession(105L, "stranger@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("không thuộc lớp học");

        // Verify the stranger email was passed
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        verify(offeringService, times(1)).joinVirtualSession(any(), emailCaptor.capture());

        assertThat(emailCaptor.getValue()).isEqualTo("stranger@test.com");

        verifyNoMoreInteractions(offeringService);
    }

    // UT-06: Học viên chưa được cấp quyền tham gia -> RuntimeException
    @Test
    void joinOnlineClassSession_UTC06_noClassAccess() {
        when(offeringService.joinVirtualSession(106L, "user_01@test.com"))
                .thenThrow(new RuntimeException("Bạn chưa được cấp quyền tham gia lớp học này."));

        assertThatThrownBy(() -> offeringService.joinVirtualSession(106L, "user_01@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("chưa được cấp quyền");

        // Verify the parameters were passed
        ArgumentCaptor<Long> sessionIdCaptor = ArgumentCaptor.forClass(Long.class);
        ArgumentCaptor<String> emailCaptor = ArgumentCaptor.forClass(String.class);
        
        verify(offeringService, times(1)).joinVirtualSession(sessionIdCaptor.capture(), emailCaptor.capture());

        assertThat(sessionIdCaptor.getValue()).isEqualTo(106L);
        assertThat(emailCaptor.getValue()).isEqualTo("user_01@test.com");

        verifyNoMoreInteractions(offeringService);
    }

    // UT-07: Staff chưa tạo Google Meet -> ResponseStatusException 409
    @Test
    void joinOnlineClassSession_UTC07_meetNotJoinable() {
        when(offeringService.joinVirtualSession(107L, "user_01@test.com"))
                .thenThrow(new ResponseStatusException(
                        org.springframework.http.HttpStatus.CONFLICT,
                        "Staff chưa tạo liên kết Google Meet cho lớp học này."));

        assertThatThrownBy(() -> offeringService.joinVirtualSession(107L, "user_01@test.com"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Staff chưa tạo liên kết Google Meet");

        // Verify the session ID was passed
        ArgumentCaptor<Long> sessionIdCaptor = ArgumentCaptor.forClass(Long.class);
        verify(offeringService, times(1)).joinVirtualSession(sessionIdCaptor.capture(), any());

        assertThat(sessionIdCaptor.getValue()).isEqualTo(107L);

        verifyNoMoreInteractions(offeringService);
    }
}
