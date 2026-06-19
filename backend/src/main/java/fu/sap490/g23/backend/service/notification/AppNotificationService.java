package fu.sap490.g23.backend.service.notification;

import fu.sap490.g23.backend.dto.response.classroom.AppNotificationResponse;

import java.util.List;

public interface AppNotificationService {

    List<AppNotificationResponse> listForUser(String userEmail);

    AppNotificationResponse markRead(Long notificationId, String userEmail);

    void markAllRead(String userEmail);

    long countUnread(String userEmail);
}
