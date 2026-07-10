package fu.sap490.g23.backend.service.notification;

import fu.sap490.g23.backend.entity.User;
import java.util.List;
import java.util.Map;

public interface ClassroomNotificationService {

    void notifyUser(User user, String type, String title, String body, Map<String, Object> metadata);
    List<User> findTrainingManagers();
    void notifyTrainingManagers(String type, String title, String body, Map<String, Object> metadata);
}
