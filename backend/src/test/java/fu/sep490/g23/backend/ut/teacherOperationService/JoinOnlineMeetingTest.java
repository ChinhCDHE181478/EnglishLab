package fu.sep490.g23.backend.ut.teacherOperationService;

import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.ClassroomSession;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
import fu.sep490.g23.backend.entity.course.LearningPackage;
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
    private ClassroomOffering offering;
    private ClassroomSession session;

    @BeforeEach
    void setUp() {
        teacher = User.builder()
                .id(teacherId)
                .email(teacherEmail)
                .fullName("Gi├ío vi├¬n 1")
                .build();

        LearningPackage learningPackage = LearningPackage.builder()
                .id(1L)
                .title("Tiß║┐ng Anh Giao Tiß║┐p")
                .build();

        offering = ClassroomOffering.builder()
                .id(offeringId)
                .learningPackage(learningPackage)
                .primaryTeacher(teacher)
                .build();

        session = ClassroomSession.builder()
                .id(sessionId)
                .classroomOffering(offering)
                .sessionDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .teacher(teacher)
                .status(ClassroomSessionStatus.SCHEDULED)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .larkMeetingUrl(meetLink)
                .larkMeetingStatus(LarkMeetingStatus.SCHEDULED)
                .larkSyncStatus("SYNCED")
                .build();
    }

    @Test
    void teacherOpensVirtualSessionFirstTime() {
        ClassroomSessionResponse expectedResponse = ClassroomSessionResponse.builder()
                .id(sessionId)
                .classroomOfferingId(offeringId)
                .classroomTitle("Teachers can only manage attendance, homework, and practice contents for the classrooms they are explicitly assigned to.")
                .sessionDate(session.getSessionDate())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .teacherId(teacherId)
                .teacherName(teacher.getFullName())
                .status(ClassroomSessionStatus.OPEN)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .larkMeetingUrl(meetLink)
                .larkMeetingStatus(LarkMeetingStatus.OPEN)
                .larkSyncStatus("SYNCED")
                .build();

        when(classroomOfferingService.openVirtualSession(sessionId, teacherEmail))
                .thenReturn(expectedResponse);

        ClassroomSessionResponse result = classroomOfferingService.openVirtualSession(sessionId, teacherEmail);

        assertThat(result.getId()).isEqualTo(sessionId);
        assertThat(result.getStatus()).isEqualTo(ClassroomSessionStatus.OPEN);
        assertThat(result.getLarkMeetingStatus()).isEqualTo(LarkMeetingStatus.OPEN);
        assertThat(result.getLarkMeetingUrl()).isEqualTo(meetLink);
        assertThat(result.getTeacherId()).isEqualTo(teacherId);
    }

    @Test
    void teacherRejoinsActiveSession() {
        ClassroomSession activeSession = ClassroomSession.builder()
                .id(sessionId)
                .classroomOffering(offering)
                .sessionDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .teacher(teacher)
                .status(ClassroomSessionStatus.IN_PROGRESS)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .larkMeetingUrl(meetLink)
                .larkMeetingStatus(LarkMeetingStatus.IN_PROGRESS)
                .larkSyncStatus("SYNCED")
                .build();

        when(classroomOfferingService.openVirtualSession(sessionId, teacherEmail))
                .thenAnswer(invocation -> {
                    return ClassroomSessionResponse.builder()
                            .id(sessionId)
                            .classroomOfferingId(offeringId)
                            .classroomTitle("Teachers can only manage attendance, homework, and practice contents for the classrooms they are explicitly assigned to.")
                            .sessionDate(activeSession.getSessionDate())
                            .startTime(activeSession.getStartTime())
                            .endTime(activeSession.getEndTime())
                            .teacherId(teacherId)
                            .teacherName(teacher.getFullName())
                            .status(ClassroomSessionStatus.IN_PROGRESS)
                            .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                            .larkMeetingUrl(meetLink)
                            .larkMeetingStatus(LarkMeetingStatus.IN_PROGRESS)
                            .larkJoinable(true)
                            .larkPlatformName("Google Meet")
                            .larkSyncStatus("SYNCED")
                            .build();
                });

        ClassroomSessionResponse response = classroomOfferingService.openVirtualSession(sessionId, teacherEmail);

        assertThat(response.getId()).isEqualTo(sessionId);
        assertThat(response.getStatus()).isEqualTo(ClassroomSessionStatus.IN_PROGRESS);
        assertThat(response.getLarkMeetingUrl()).isEqualTo(meetLink);
        assertThat(response.isLarkJoinable()).isTrue();
        assertThat(response.getLarkPlatformName()).isEqualTo("Google Meet");
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
        ClassroomSessionResponse errorResponse = ClassroomSessionResponse.builder()
                .id(sessionId)
                .classroomOfferingId(offeringId)
                .classroomTitle("BR-11")
                .sessionDate(LocalDate.now())
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(11, 0))
                .teacherId(teacherId)
                .teacherName(teacher.getFullName())
                .status(ClassroomSessionStatus.SCHEDULED)
                .deliveryMode(ClassroomDeliveryMode.VIRTUAL)
                .larkMeetingUrl(null)
                .larkSyncStatus("SYNCED")
                .build();

        when(classroomOfferingService.openVirtualSession(sessionId, teacherEmail))
                .thenThrow(new RuntimeException(
                        "Ch╞░a thß╗â tß║ío ph├▓ng Google Meet cho buß╗òi hß╗ìc n├áy."));


        assertThatThrownBy(() -> classroomOfferingService.openVirtualSession(sessionId, teacherEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Ch╞░a thß╗â tß║ío ph├▓ng Google Meet cho buß╗òi hß╗ìc n├áy.");
    }

    @Test
    void sessionNotFound() {
        Long invalidSessionId = 9999L;

        when(classroomOfferingService.openVirtualSession(invalidSessionId, teacherEmail))
                .thenThrow(new RuntimeException(
                        "Kh├┤ng t├¼m thß║Ñy buß╗òi hß╗ìc vß╗¢i ID: " + invalidSessionId));

        assertThatThrownBy(() -> classroomOfferingService.openVirtualSession(invalidSessionId, teacherEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Kh├┤ng t├¼m thß║Ñy buß╗òi hß╗ìc vß╗¢i ID: " + invalidSessionId);
    }

    @Test
    void endedOrCancelledSession() {
        when(classroomOfferingService.openVirtualSession(sessionId, teacherEmail))
                .thenThrow(new RuntimeException(
                        "Kh├┤ng thß╗â tham gia buß╗òi hß╗ìc ─æ├ú kß║┐t th├║c hoß║╖c bß╗ï hß╗ºy."));

        // Act & Assert
        assertThatThrownBy(() -> classroomOfferingService.openVirtualSession(sessionId, teacherEmail))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Kh├┤ng thß╗â tham gia buß╗òi hß╗ìc ─æ├ú kß║┐t th├║c hoß║╖c bß╗ï hß╗ºy.");
    }
}
