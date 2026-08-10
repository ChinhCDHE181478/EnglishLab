package fu.sap490.g23.backend.service.auth;

import fu.sap490.g23.backend.dto.request.GoogleAuthRequest;
import fu.sap490.g23.backend.dto.response.AuthResponse;

public interface GoogleAuthService {

    AuthResponse loginWithGoogle(GoogleAuthRequest request);
}
