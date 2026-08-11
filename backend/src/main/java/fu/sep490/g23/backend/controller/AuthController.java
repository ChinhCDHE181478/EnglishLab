package fu.sep490.g23.backend.controller;

import fu.sep490.g23.backend.dto.request.EmailRequest;
import fu.sep490.g23.backend.dto.request.FacebookAuthRequest;
import fu.sep490.g23.backend.dto.request.GoogleAuthRequest;
import fu.sep490.g23.backend.dto.request.LoginRequest;
import fu.sep490.g23.backend.dto.request.ResetPasswordRequest;
import fu.sep490.g23.backend.dto.request.RegisterRequest;
import fu.sep490.g23.backend.dto.request.VerifyEmailRequest;
import fu.sep490.g23.backend.dto.response.AuthResponse;
import fu.sep490.g23.backend.service.auth.FacebookAuthService;
import fu.sep490.g23.backend.service.auth.GoogleAuthService;
import fu.sep490.g23.backend.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final FacebookAuthService facebookAuthService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
    public ResponseEntity<AuthResponse> verifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        AuthResponse response = authService.verifyEmail(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<AuthResponse> resendVerification(@Valid @RequestBody EmailRequest request) {
        AuthResponse response = authService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@Valid @RequestBody EmailRequest request) {
        AuthResponse response = authService.forgotPassword(request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        AuthResponse response = authService.resetPassword(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        AuthResponse response = googleAuthService.loginWithGoogle(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/facebook")
    public ResponseEntity<AuthResponse> facebookLogin(@Valid @RequestBody FacebookAuthRequest request) {
        AuthResponse response = facebookAuthService.loginWithFacebook(request.getAccessToken());
        return ResponseEntity.ok(response);
    }
}
