package fu.sap490.g23.backend.service.notification.impl;

import fu.sap490.g23.backend.dto.request.UpdateNotificationPreferenceRequest;
import fu.sap490.g23.backend.dto.response.NotificationPreferenceResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.notification.NotificationPreference;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.notification.NotificationPreferenceRepository;
import fu.sap490.g23.backend.service.notification.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getForUser(String userEmail) {
        User user = requireUser(userEmail);
        return preferenceRepository.findByUserId(user.getId())
                .map(this::toResponse)
                .orElseGet(this::defaultResponse);
    }

    @Override
    public NotificationPreferenceResponse updateForUser(
            String userEmail,
            UpdateNotificationPreferenceRequest request
    ) {
        User user = requireUser(userEmail);
        NotificationPreference preference = preferenceRepository.findByUserId(user.getId())
                .orElseGet(() -> NotificationPreference.builder().user(user).build());
        preference.setEmailEnabled(request.getEmailEnabled());
        preference.setInAppEnabled(request.getInAppEnabled());
        if (request.getClassReminderEnabled() != null) {
            preference.setClassReminderEnabled(request.getClassReminderEnabled());
        }
        if (request.getStudyAlertEnabled() != null) {
            preference.setStudyAlertEnabled(request.getStudyAlertEnabled());
        }
        return toResponse(preferenceRepository.save(preference));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailEnabled(User user) {
        return isEnabled(user, NotificationPreference::isEmailEnabled);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInAppEnabled(User user) {
        return isEnabled(user, NotificationPreference::isInAppEnabled);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isClassReminderEnabled(User user) {
        return isEnabled(user, NotificationPreference::isClassReminderEnabled);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStudyAlertEnabled(User user) {
        return isEnabled(user, NotificationPreference::isStudyAlertEnabled);
    }

    private boolean isEnabled(
            User user,
            java.util.function.Predicate<NotificationPreference> preferenceSelector
    ) {
        if (user == null || user.getId() == null) {
            return true;
        }
        return preferenceRepository.findByUserId(user.getId())
                .map(preferenceSelector::test)
                .orElse(true);
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));
    }

    private NotificationPreferenceResponse toResponse(NotificationPreference preference) {
        return NotificationPreferenceResponse.builder()
                .emailEnabled(preference.isEmailEnabled())
                .inAppEnabled(preference.isInAppEnabled())
                .classReminderEnabled(preference.isClassReminderEnabled())
                .studyAlertEnabled(preference.isStudyAlertEnabled())
                .build();
    }

    private NotificationPreferenceResponse defaultResponse() {
        return NotificationPreferenceResponse.builder()
                .emailEnabled(true)
                .inAppEnabled(true)
                .classReminderEnabled(true)
                .studyAlertEnabled(true)
                .build();
    }
}
