package fu.sep490.g23.backend.service.notification;

import fu.sep490.g23.backend.dto.request.UpdateNotificationPreferenceRequest;
import fu.sep490.g23.backend.dto.response.NotificationPreferenceResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.notification.NotificationPreference;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.notification.NotificationPreferenceRepository;
import fu.sep490.g23.backend.service.notification.impl.NotificationPreferenceServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceImplTest {

    @Mock private NotificationPreferenceRepository preferenceRepository;
    @Mock private UserRepository userRepository;

    private NotificationPreferenceServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new NotificationPreferenceServiceImpl(preferenceRepository, userRepository);
        user = User.builder().id(12L).email("learner@test.vn").fullName("Learner").build();
    }

    @Test
    void getForUser_WithoutStoredPreference_DefaultsBothChannelsToEnabled() {
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(user.getId())).thenReturn(Optional.empty());

        NotificationPreferenceResponse response = service.getForUser(user.getEmail());

        assertTrue(response.isEmailEnabled());
        assertTrue(response.isInAppEnabled());
        assertTrue(response.isClassReminderEnabled());
        assertTrue(response.isStudyAlertEnabled());
    }

    @Test
    void updateForUser_PersistsBothChannelChoices() {
        UpdateNotificationPreferenceRequest request = new UpdateNotificationPreferenceRequest();
        request.setEmailEnabled(false);
        request.setInAppEnabled(false);
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(preferenceRepository.findByUserId(user.getId())).thenReturn(Optional.empty());
        when(preferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        NotificationPreferenceResponse response = service.updateForUser(user.getEmail(), request);

        assertFalse(response.isEmailEnabled());
        assertFalse(response.isInAppEnabled());
        assertTrue(response.isClassReminderEnabled());
        assertTrue(response.isStudyAlertEnabled());
        verify(preferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    void channelChecks_UseStoredPreference() {
        NotificationPreference preference = NotificationPreference.builder()
                .user(user)
                .emailEnabled(false)
                .inAppEnabled(true)
                .build();
        when(preferenceRepository.findByUserId(user.getId())).thenReturn(Optional.of(preference));

        assertFalse(service.isEmailEnabled(user));
        assertTrue(service.isInAppEnabled(user));
    }
}
