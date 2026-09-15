package fu.sep490.g23.backend.ut.teacherOperationService;

import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.GoogleMeetStatus;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JoinOnlineMeetingTest {

    private static final Long teacherId = 100L;
    private static final Long sessionId = 500L;
    private static final Long offeringId = 200L;
    private static final String teacherEmail = "teacher@englishlab.com";
    private static final String meetLink = "https://meet.google.com/abc-defg-hij";

    @Mock
    private ClassroomOfferingService classroomOfferingService;

    private User teacher;
    private ClassSection offering;
    private ClassSchedule session;

    @BeforeEach
    void setUp() {
        teacher = User.builder()
                .id(teacherId)
                .email(teacherEmail)
                .fullName("Giáo viên 1")
                .build();

        offering = ClassSection.builder()
                .id(offeringId)
                .primaryTeacher(teacher)
                .googleMeetUrl(meetLink)
                .googleMeetStatus(GoogleMeetStatus.READY)
                .build();

        session = ClassSchedule.builder()
                .id(sessionId)
                .classSection(offering)
                .sessionDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .teacher(teacher)
                .status(ClassroomSessionStatus.SCHEDULED)
                .deliveryModeOverride(ClassroomDeliveryMode.VIRTUAL)
                .build();
    }

    @Test
    void teacherOpensVirtualSessionFirstTime() {
        ClassroomSessionResponse expectedResponse = ClassroomSessionResponse.builder()
                .id(sessionId)
                .classSectionId(offeringId)
                .classroomTitle("Teachers can only manage attendance, homework, and practice contents for the classrooms they are explicitly assigned to.")
                .sessionDate(session.getSessionDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .teacherId(teacherId)
                .teacherName(teacher.getFullName())
                .status(ClassroomSessionStatus.OPEN)
                .effectiveDeliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .googleMeetUrl(meetLink)
                .googleMeetStatus(GoogleMeetStatus.READY)
                .build();

        when(classroomOfferingService.openVirtualSession(sessionId, teacherEmail))
                .thenReturn(expectedResponse);

        ClassroomSessionResponse result = classroomOfferingService.openVirtualSession(sessionId, teacherEmail);

        assertThat(result.getId()).isEqualTo(sessionId);
        assertThat(result.getStatus()).isEqualTo(ClassroomSessionStatus.OPEN);
        assertThat(result.getGoogleMeetStatus()).isEqualTo(GoogleMeetStatus.READY);
        assertThat(result.getGoogleMeetUrl()).isEqualTo(meetLink);
        assertThat(result.getTeacherId()).isEqualTo(teacherId);
    }

    @Test
    void teacherRejoinsActiveSession() {
        ClassSchedule activeSession = ClassSchedule.builder()
                .id(sessionId)
                .classSection(offering)
                .sessionDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .teacher(teacher)
                .status(ClassroomSessionStatus.IN_PROGRESS)
                .deliveryModeOverride(ClassroomDeliveryMode.VIRTUAL)
                .build();

        when(classroomOfferingService.openVirtualSession(sessionId, teacherEmail))
                .thenAnswer(invocation -> ClassroomSessionResponse.builder()
                        .id(sessionId)
                        .classSectionId(offeringId)
                        .classroomTitle("Teachers can only manage attendance, homework, and practice contents for the classrooms they are explicitly assigned to.")
                        .sessionDate(activeSession.getSessionDate())
                        .startTime(activeSession.getStartTime())
                        .endTime(activeSession.getEndTime())
                        .teacherId(teacherId)
                        .teacherName(teacher.getFullName())
                        .status(ClassroomSessionStatus.IN_PROGRESS)
                        .effectiveDeliveryMode(ClassroomDeliveryMode.VIRTUAL)
                        .googleMeetUrl(meetLink)
                        .googleMeetStatus(GoogleMeetStatus.READY)
                        .googleMeetJoinable(true)
                        .build());

        ClassroomSessionResponse response = classroomOfferingService.openVirtualSession(sessionId, teacherEmail);

        assertThat(response.getId()).isEqualTo(sessionId);
        assertThat(response.getStatus()).isEqualTo(ClassroomSessionStatus.IN_PROGRESS);
        assertThat(response.getGoogleMeetUrl()).isEqualTo(meetLink);
        assertThat(response.isGoogleMeetJoinable()).isTrue();
    }

    @Test
    void teacherWithoutAuthorization() {
        // Arrange
        String unauthorizedEmail = "unauthorized@gmail.com";

        when(classroomOfferingService.openVirtualSession(sessionId, unauthorizedEmail))
                .thenThrow(new RuntimeException(
                        "Access Denied. You are not assigned to manage this classroom."));

        assertThatThrownBy(() -> classroomOfferingService.openVirtualSession(sessionId, unauthorizedEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Access Denied. You are not assigned to manage this classroom.");
    }

    @Test
    void sessionWithNoValidMeetLink() {
        when(classroomOfferingService.openVirtualSession(sessionId, teacherEmail))
                .thenThrow(new RuntimeException(
                        "Chưa thể tạo phòng Google Meet cho buổi học này."));

        assertThatThrownBy(() -> classroomOfferingService.openVirtualSession(sessionId, teacherEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Chưa thể tạo phòng Google Meet cho buổi học này.");
    }

    @Test
    void sessionNotFound() {
        Long invalidSessionId = 9999L;

        when(classroomOfferingService.openVirtualSession(invalidSessionId, teacherEmail))
                .thenThrow(new RuntimeException(
                        "Không tìm thấy buổi học với ID: " + invalidSessionId));

        assertThatThrownBy(() -> classroomOfferingService.openVirtualSession(invalidSessionId, teacherEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Không tìm thấy buổi học với ID: " + invalidSessionId);
    }

    @Test
    void endedOrCancelledSession() {
        when(classroomOfferingService.openVirtualSession(sessionId, teacherEmail))
                .thenThrow(new RuntimeException(
                        "Không thể tham gia buổi học đã kết thúc hoặc bị hủy."));

        // Act & Assert
        assertThatThrownBy(() -> classroomOfferingService.openVirtualSession(sessionId, teacherEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Không thể tham gia buổi học đã kết thúc hoặc bị hủy.");
    }
}
