package fu.sap490.g23.backend.service.mail;

import fu.sap490.g23.backend.entity.User;

public interface LearningReminderMailService {
    void sendReminder(User user, String subject, String heading, String message, String actionPath);
}
