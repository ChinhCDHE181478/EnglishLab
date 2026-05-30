package fu.sap490.g23.backend.service;

import fu.sap490.g23.backend.dto.request.LoginRequest;
import fu.sap490.g23.backend.dto.request.RegisterRequest;
import fu.sap490.g23.backend.dto.response.AuthResponse;

public interface IAuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}
