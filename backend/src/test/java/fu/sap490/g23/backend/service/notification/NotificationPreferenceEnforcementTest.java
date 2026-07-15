package fu.sap490.g23.backend.service.notification;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.notification.AppNotificationRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.classroom.ClassroomMapper;
import fu.sap490.g23.backend.service.mail.impl.ClassroomHomeworkMailServiceImpl;
import fu.sap490.g23.backend.service.mail.impl.CourseEnrollmentMailServiceImpl;
import fu.sap490.g23.backend.service.notification.impl.AppNotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceEnforcementTest {

    @Mock private AppNotificationRepository notificationRepository;
    @Mock private ClassroomAccessHelper accessHelper;
    @Mock private ClassroomMapper classroomMapper;
    @Mock private NotificationPreferenceService preferenceService;
    @Mock private JavaMailSender mailSender;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(22L).email("learner@test.vn").fullName("Learner").build();
    }

    @Test
    void createForUser_WhenInAppDisabled_DoesNotPersistNotification() {
        AppNotificationServiceImpl service = appNotificationService();
        when(preferenceService.isInAppEnabled(user)).thenReturn(false);

        service.createForUser(user, "HOMEWORK", "Bài tập mới", "Bạn có bài tập mới.", Map.of("id", 1L));

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createForUser_WhenInAppEnabled_PersistsNotification() {
        AppNotificationServiceImpl service = appNotificationService();
        when(preferenceService.isInAppEnabled(user)).thenReturn(true);

        service.createForUser(user, "HOMEWORK", "Bài tập mới", "Bạn có bài tập mới.", Map.of("id", 1L));

        verify(notificationRepository).save(any());
    }

    @Test
    void businessMailServices_WhenEmailDisabled_DoNotCreateMailMessages() {
        when(preferenceService.isEmailEnabled(user)).thenReturn(false);
        CourseEnrollmentMailServiceImpl enrollmentMailService =
                new CourseEnrollmentMailServiceImpl(mailSender, preferenceService);
        ClassroomHomeworkMailServiceImpl homeworkMailService =
                new ClassroomHomeworkMailServiceImpl(mailSender, preferenceService);

        enrollmentMailService.sendEnrollmentSuccessEmail(user, null, null);
        homeworkMailService.sendHomeworkAssigned(user, null);

        verifyNoInteractions(mailSender);
    }

    private AppNotificationServiceImpl appNotificationService() {
        return new AppNotificationServiceImpl(
                notificationRepository,
                accessHelper,
                classroomMapper,
                preferenceService
        );
    }
}
