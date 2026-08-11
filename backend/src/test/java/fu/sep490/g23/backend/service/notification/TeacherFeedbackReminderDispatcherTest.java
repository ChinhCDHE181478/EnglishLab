package fu.sep490.g23.backend.service.notification;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sep490.g23.backend.service.mail.LearningReminderMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherFeedbackReminderDispatcherTest {

    @Mock private ClassroomOfferingRepository offeringRepository;
    @Mock private ClassroomEnrollmentRepository enrollmentRepository;
    @Mock private AppNotificationService notificationService;
    @Mock private NotificationPreferenceService preferenceService;
    @Mock private LearningReminderMailService mailService;

    private TeacherFeedbackReminderDispatcher dispatcher;
    private User learner;
    private ClassroomOffering classroom;

    @BeforeEach
    void setUp() {
        dispatcher = new TeacherFeedbackReminderDispatcher(
                offeringRepository,
                enrollmentRepository,
                notificationService,
                preferenceService,
                mailService
        );
        learner = User.builder().id(5L).email("learner@example.com").fullName("Học viên").build();
        classroom = ClassroomOffering.builder()
                .id(9L)
                .endDate(LocalDate.now())
                .learningPackage(LearningPackage.builder().title("IELTS Evening").build())
                .build();
    }

    @Test
    void notificationTransactionFailureIsContainedAndDoesNotSendEmail() {
        when(offeringRepository.findById(9L)).thenReturn(Optional.of(classroom));
        when(enrollmentRepository.findByClassroomOfferingIdAndRegistrationStatusIn(any(), any()))
                .thenReturn(List.of(ClassroomEnrollment.builder().student(learner).build()));
        when(preferenceService.isStudyAlertEnabled(learner)).thenReturn(true);
        when(notificationService.createForUserOnce(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("isolated transaction rolled back"));

        dispatcher.dispatchForClassroom(9L, LocalDate.now(), 7, 14, 2);

        verify(mailService, never()).sendReminder(any(), any(), any(), any(), any());
    }

    @Test
    void mailFailureDoesNotAbortClassroomDispatch() {
        when(offeringRepository.findById(9L)).thenReturn(Optional.of(classroom));
        when(enrollmentRepository.findByClassroomOfferingIdAndRegistrationStatusIn(any(), any()))
                .thenReturn(List.of(ClassroomEnrollment.builder().student(learner).build()));
        when(preferenceService.isStudyAlertEnabled(learner)).thenReturn(true);
        when(preferenceService.isEmailEnabled(learner)).thenReturn(true);
        when(notificationService.createForUserOnce(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);
        doThrow(new RuntimeException("mail unavailable"))
                .when(mailService).sendReminder(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> dispatcher.dispatchForClassroom(9L, LocalDate.now(), 7, 14, 2));
    }
}
