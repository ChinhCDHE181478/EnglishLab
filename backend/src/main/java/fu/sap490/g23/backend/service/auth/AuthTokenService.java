package fu.sap490.g23.backend.service.auth;

import fu.sap490.g23.backend.entity.AuthToken;
import fu.sap490.g23.backend.entity.enums.AuthTokenType;
import fu.sap490.g23.backend.entity.User;

public interface AuthTokenService {

    AuthToken issueEmailVerificationToken(User user);

    AuthToken issueEmailVerificationTokenForRegistration(User user);

    AuthToken issuePasswordResetToken(User user);

    AuthToken requireValidEmailVerificationCode(User user, String rawCode, String invalidMessage);

    AuthToken requireValidPasswordResetCode(User user, String rawCode, String invalidMessage);

    void markUsed(AuthToken token);

    void deleteTokens(User user, AuthTokenType type);
}
