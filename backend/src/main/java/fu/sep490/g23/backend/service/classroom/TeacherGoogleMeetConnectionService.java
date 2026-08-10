package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.response.teacher.TeacherGoogleMeetConnectionResponse;
import fu.sap490.g23.backend.entity.User;

public interface TeacherGoogleMeetConnectionService {
    TeacherGoogleMeetConnectionResponse getConnection(String teacherEmail);
    String createAuthorizationUrl(String teacherEmail);
    String completeAuthorization(String code, String state);
    void disconnect(String teacherEmail);
    String requireRefreshToken(User teacher);
    void markReauthenticationRequired(User teacher);
}
