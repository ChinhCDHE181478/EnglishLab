package fu.sap490.g23.backend.service.mail;

import fu.sap490.g23.backend.entity.User;

public interface AuthMailService {

    void sendVerificationEmail(User user, String code);
    void sendPasswordResetEmail(User user, String code);
    void sendStaffCreatedAccountEmail(User user, String code);
    void sendTeacherGoogleMeetInvitation(User user);
}
