package fu.sap490.g23.backend.service.auth;

import fu.sap490.g23.backend.dto.request.LoginRequest;
import fu.sap490.g23.backend.dto.request.RegisterRequest;
import fu.sap490.g23.backend.dto.request.ResetPasswordRequest;
import fu.sap490.g23.backend.dto.request.VerifyEmailRequest;
import fu.sap490.g23.backend.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);

    AuthResponse verifyEmail(VerifyEmailRequest request);

    AuthResponse resendVerificationEmail(String email);

    AuthResponse forgotPassword(String email);

    AuthResponse resetPassword(ResetPasswordRequest request);
}
