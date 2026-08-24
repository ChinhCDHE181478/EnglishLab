package fu.sep490.g23.backend.service.notification;

import fu.sep490.g23.backend.dto.response.classroom.AppNotificationResponse;
import fu.sep490.g23.backend.entity.User;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AppNotificationService {

    List<AppNotificationResponse> listForUser(String userEmail);

    Page<AppNotificationResponse> pageForUser(String userEmail, Pageable pageable);

    AppNotificationResponse markRead(Long notificationId, String userEmail);

    void markAllRead(String userEmail);

    long countUnread(String userEmail);

    void createForUser(User user, String type, String title, String body, Map<String, Object> metadata);

    boolean createForUserOnce(
            User user,
            String type,
            String title,
            String body,
            String actionPath,
            String deduplicationKey,
            Map<String, Object> metadata
    );
}
