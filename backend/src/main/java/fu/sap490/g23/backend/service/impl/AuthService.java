package fu.sap490.g23.backend.service.impl;

import fu.sap490.g23.backend.dto.request.LoginRequest;
import fu.sap490.g23.backend.dto.request.ResetPasswordRequest;
import fu.sap490.g23.backend.dto.request.RegisterRequest;
import fu.sap490.g23.backend.dto.response.AuthResponse;
import fu.sap490.g23.backend.dto.response.UserResponse;
import fu.sap490.g23.backend.entity.AuthToken;
import fu.sap490.g23.backend.entity.AuthTokenType;
import fu.sap490.g23.backend.entity.Role;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.security.JwtService;
import fu.sap490.g23.backend.service.IAuthService;
import fu.sap490.g23.backend.service.mail.AuthMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuthTokenService authTokenService;
    private final AuthMailService authMailService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new RuntimeException("Email này đã được đăng ký.");
        }

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .email(normalizedEmail)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.LEARNER)
                .emailVerified(false)
                .build();

        User savedUser = userRepository.save(user);
        AuthToken verificationToken = authTokenService.issueEmailVerificationToken(savedUser);
        authMailService.sendVerificationEmail(savedUser, verificationToken.getToken());

        return AuthResponse.builder()
                .message("Đăng ký thành công. Vui lòng kiểm tra email để xác thực tài khoản.")
                .user(toUserResponse(savedUser))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng."));

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Tài khoản của bạn chưa xác thực email. Vui lòng kiểm tra hộp thư và thử lại.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getEmail(), request.getPassword())
        );

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .message("Đăng nhập thành công.")
                .accessToken(token)
                .tokenType("Bearer")
                .user(toUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse verifyEmail(String token) {
        AuthToken verificationToken = authTokenService.requireValidToken(
                token,
                AuthTokenType.EMAIL_VERIFICATION,
                "Liên kết xác thực không hợp lệ hoặc đã hết hạn."
        );

        User user = verificationToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
        authTokenService.markUsed(verificationToken);

        return AuthResponse.builder()
                .message("Xác thực email thành công. Bạn có thể đăng nhập ngay bây giờ.")
                .user(toUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse resendVerificationEmail(String email) {
        User user = userRepository.findByEmail(email.trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với email này."));

        if (user.isEmailVerified()) {
            return AuthResponse.builder()
                    .message("Email này đã được xác thực rồi.")
                    .user(toUserResponse(user))
                    .build();
        }

        AuthToken verificationToken = authTokenService.issueEmailVerificationToken(user);
        authMailService.sendVerificationEmail(user, verificationToken.getToken());

        return AuthResponse.builder()
                .message("Đã gửi lại email xác thực. Vui lòng kiểm tra hộp thư của bạn.")
                .user(toUserResponse(user))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse forgotPassword(String email) {
        userRepository.findByEmail(email.trim().toLowerCase()).ifPresent((user) -> {
            AuthToken resetToken = authTokenService.issuePasswordResetToken(user);
            authMailService.sendPasswordResetEmail(user, resetToken.getToken());
        });

        return AuthResponse.builder()
                .message("Nếu email tồn tại trong hệ thống, chúng tôi đã gửi hướng dẫn đặt lại mật khẩu.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        AuthToken resetToken = authTokenService.requireValidToken(
                request.getToken(),
                AuthTokenType.PASSWORD_RESET,
                "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn."
        );

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setEmailVerified(true);
        userRepository.save(user);
        authTokenService.markUsed(resetToken);

        return AuthResponse.builder()
                .message("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập với mật khẩu mới.")
                .user(toUserResponse(user))
                .build();
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .phoneNumber(user.getPhoneNumber())
                .targetExam(user.getTargetExam())
                .targetScore(user.getTargetScore())
                .currentBand(user.getCurrentBand())
                .studyGoal(user.getStudyGoal())
                .profileCompleted(user.isProfileCompleted())
                .build();
    }
}
