package fu.sap490.g23.backend.service.notification;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.notification.AppNotification;
import fu.sap490.g23.backend.repository.notification.AppNotificationRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.classroom.ClassroomMapper;
import fu.sap490.g23.backend.service.notification.impl.AppNotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppNotificationServiceImplTest {
    @Mock private AppNotificationRepository repository;
    @Mock private ClassroomAccessHelper accessHelper;
    @Mock private ClassroomMapper classroomMapper;
    @Mock private NotificationPreferenceService preferenceService;

    private AppNotificationServiceImpl service;
    private User learner;

    @BeforeEach
    void setUp() {
        service = new AppNotificationServiceImpl(repository, accessHelper, classroomMapper, preferenceService);
        learner = User.builder().id(9L).email("learner@englishlab.vn").fullName("Học viên").build();
    }

    @Test
    void createForUserOnce_WhenKeyAlreadyExists_DoesNotCreateDuplicate() {
        when(preferenceService.isInAppEnabled(learner)).thenReturn(true);
        when(repository.existsByUserIdAndDeduplicationKey(9L, "SESSION_4_2H")).thenReturn(true);

        boolean created = service.createForUserOnce(
                learner, "CLASS_REMINDER", "Sắp học", "Nội dung",
                "/my-classrooms/2", "SESSION_4_2H", Map.of("sessionId", 4L));

        assertFalse(created);
        verify(repository, never()).save(any());
    }

    @Test
    void createForUserOnce_PersistsActionAndMetadata() {
        when(preferenceService.isInAppEnabled(learner)).thenReturn(true);
        when(repository.existsByUserIdAndDeduplicationKey(9L, "HOMEWORK_6_24H")).thenReturn(false);

        boolean created = service.createForUserOnce(
                learner, "HOMEWORK_DEADLINE", "Sắp hết hạn", "Nội dung",
                "/my-homework", "HOMEWORK_6_24H", Map.of("homeworkId", 6L));

        assertTrue(created);
        ArgumentCaptor<AppNotification> captor = ArgumentCaptor.forClass(AppNotification.class);
        verify(repository).save(captor.capture());
        assertEquals("/my-homework", captor.getValue().getActionPath());
        assertEquals("HOMEWORK_6_24H", captor.getValue().getDeduplicationKey());
        assertTrue(captor.getValue().getMetadataJson().contains("\"homeworkId\":6"));
    }
}
