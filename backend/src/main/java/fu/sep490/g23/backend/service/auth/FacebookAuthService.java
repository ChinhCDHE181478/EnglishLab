package fu.sep490.g23.backend.service.auth;

import fu.sep490.g23.backend.dto.response.AuthResponse;

public interface FacebookAuthService {

    AuthResponse loginWithFacebook(String accessToken);
}
