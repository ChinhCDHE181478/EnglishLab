package fu.sap490.g23.backend.service.notification;

import fu.sap490.g23.backend.dto.request.UpdateNotificationPreferenceRequest;
import fu.sap490.g23.backend.dto.response.NotificationPreferenceResponse;
import fu.sap490.g23.backend.entity.User;

public interface NotificationPreferenceService {

    NotificationPreferenceResponse getForUser(String userEmail);

    NotificationPreferenceResponse updateForUser(String userEmail, UpdateNotificationPreferenceRequest request);

    boolean isEmailEnabled(User user);

    boolean isInAppEnabled(User user);

    boolean isClassReminderEnabled(User user);

    boolean isStudyAlertEnabled(User user);
}
