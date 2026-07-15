package fu.sap490.g23.backend.service.notification;

import fu.sap490.g23.backend.dto.response.classroom.AppNotificationResponse;
import fu.sap490.g23.backend.entity.User;
import java.util.List;
import java.util.Map;

public interface AppNotificationService {

    List<AppNotificationResponse> listForUser(String userEmail);

    AppNotificationResponse markRead(Long notificationId, String userEmail);

    void markAllRead(String userEmail);

    long countUnread(String userEmail);

    void createForUser(User user, String type, String title, String body, Map<String, Object> metadata);
}
