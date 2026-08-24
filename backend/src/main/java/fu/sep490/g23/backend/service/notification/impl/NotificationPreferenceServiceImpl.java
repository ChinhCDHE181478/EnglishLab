package fu.sep490.g23.backend.service.notification.impl;

import fu.sep490.g23.backend.dto.request.UpdateNotificationPreferenceRequest;
import fu.sep490.g23.backend.dto.response.NotificationPreferenceResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.service.notification.NotificationPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationPreferenceServiceImpl implements NotificationPreferenceService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public NotificationPreferenceResponse getForUser(String userEmail) {
        User user = requireUser(userEmail);
        return toResponse(user);
    }

    @Override
    public NotificationPreferenceResponse updateForUser(
            String userEmail,
            UpdateNotificationPreferenceRequest request
    ) {
        User user = requireUser(userEmail);
        user.setNotificationEmailEnabled(request.getEmailEnabled());
        user.setNotificationInAppEnabled(request.getInAppEnabled());
        if (request.getClassReminderEnabled() != null) {
            user.setNotificationClassReminderEnabled(request.getClassReminderEnabled());
        }
        if (request.getStudyAlertEnabled() != null) {
            user.setNotificationStudyAlertEnabled(request.getStudyAlertEnabled());
        }
        return toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isEmailEnabled(User user) {
        return user == null || user.isNotificationEmailEnabled();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isInAppEnabled(User user) {
        return user == null || user.isNotificationInAppEnabled();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isClassReminderEnabled(User user) {
        return user == null || user.isNotificationClassReminderEnabled();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isStudyAlertEnabled(User user) {
        return user == null || user.isNotificationStudyAlertEnabled();
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản."));
    }

    private NotificationPreferenceResponse toResponse(User user) {
        return NotificationPreferenceResponse.builder()
                .emailEnabled(user.isNotificationEmailEnabled())
                .inAppEnabled(user.isNotificationInAppEnabled())
                .classReminderEnabled(user.isNotificationClassReminderEnabled())
                .studyAlertEnabled(user.isNotificationStudyAlertEnabled())
                .build();
    }
}
