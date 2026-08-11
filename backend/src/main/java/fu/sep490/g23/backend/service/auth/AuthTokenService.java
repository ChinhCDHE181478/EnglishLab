package fu.sep490.g23.backend.service.auth;

import fu.sep490.g23.backend.entity.AuthToken;
import fu.sep490.g23.backend.entity.enums.AuthTokenType;
import fu.sep490.g23.backend.entity.User;

public interface AuthTokenService {

    AuthToken issueEmailVerificationToken(User user);

    AuthToken issueEmailVerificationTokenForRegistration(User user);

    AuthToken issuePasswordResetToken(User user);

    AuthToken issueGoogleMeetConnectionState(User user);

    AuthToken requireValidEmailVerificationCode(User user, String rawCode, String invalidMessage);

    AuthToken requireValidPasswordResetCode(User user, String rawCode, String invalidMessage);

    AuthToken requireValidGoogleMeetConnectionState(String rawState);

    void markUsed(AuthToken token);

    void deleteTokens(User user, AuthTokenType type);
}
