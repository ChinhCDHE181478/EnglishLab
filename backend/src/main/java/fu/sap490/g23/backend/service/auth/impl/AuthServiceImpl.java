package fu.sap490.g23.backend.service.auth.impl;

import fu.sap490.g23.backend.service.auth.*;

import fu.sap490.g23.backend.dto.request.LoginRequest;
import fu.sap490.g23.backend.dto.request.RegisterRequest;
import fu.sap490.g23.backend.dto.request.ResetPasswordRequest;
import fu.sap490.g23.backend.dto.request.VerifyEmailRequest;
import fu.sap490.g23.backend.dto.response.AuthResponse;
import fu.sap490.g23.backend.dto.response.UserResponse;
import fu.sap490.g23.backend.entity.AuthToken;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sap490.g23.backend.security.JwtService;
import fu.sap490.g23.backend.service.mail.AuthMailService;
import fu.sap490.g23.backend.service.user.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PlacementTestAttemptRepository placementTestAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final AuthTokenService authTokenService;
    private final AuthMailService authMailService;
    private final UserRoleService userRoleService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail).orElse(null);

        if (user != null && user.isEmailVerified()) {
            throw new RuntimeException("Email này đã được đăng ký.");
        }

        if (user == null) {
            user = User.builder()
                    .fullName(request.getFullName().trim())
                    .email(normalizedEmail)
                    .password(passwordEncoder.encode(request.getPassword()))
                    .emailVerified(false)
                    .build();
            userRoleService.assignRole(user, RoleEnum.LEARNER);
        } else {
            user.setFullName(request.getFullName().trim());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            userRoleService.replaceRoles(user, RoleEnum.LEARNER);
            user.setEmailVerified(false);
        }

        User savedUser = userRepository.save(user);
        AuthToken verificationToken = authTokenService.issueEmailVerificationTokenForRegistration(savedUser);
        authMailService.sendVerificationEmail(savedUser, verificationToken.getToken());

        return AuthResponse.builder()
                .message("Đăng ký thành công. Vui lòng kiểm tra email và nhập mã xác thực để kích hoạt tài khoản.")
                .user(toUserResponse(savedUser))
                .build();
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Email hoặc mật khẩu không đúng."));

        if (!user.isEmailVerified()) {
            throw new RuntimeException("Tài khoản của bạn chưa xác thực email. Vui lòng kiểm tra hộp thư, nhập mã xác thực rồi thử lại.");
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
    public AuthResponse verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản với email này."));

        if (user.isEmailVerified()) {
            return AuthResponse.builder()
                    .message("Email này đã được xác thực rồi.")
                    .user(toUserResponse(user))
                    .build();
        }

        AuthToken verificationToken = authTokenService.requireValidEmailVerificationCode(
                user,
                request.getCode(),
                "Mã xác thực không hợp lệ hoặc đã hết hạn."
        );

        return completeEmailVerification(verificationToken);
    }

    private AuthResponse completeEmailVerification(AuthToken verificationToken) {
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
                .message("Đã gửi lại mã xác thực. Vui lòng kiểm tra hộp thư của bạn.")
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
                .message("Nếu email tồn tại trong hệ thống, chúng tôi đã gửi mã OTP đặt lại mật khẩu.")
                .build();
    }

    @Override
    @Transactional
    public AuthResponse resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail().trim().toLowerCase())
                .orElseThrow(() -> new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn."));

        AuthToken resetToken = authTokenService.requireValidPasswordResetCode(
                user,
                request.getCode(),
                "Mã OTP không hợp lệ hoặc đã hết hạn."
        );

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
                .roles(user.getRoleCodes().stream().map(Enum::name).sorted().toList())
                .phoneNumber(user.getPhoneNumber())
                .targetExam(user.getTargetExam())
                .targetScore(user.getTargetScore())
                .currentBand(user.getCurrentBand())
                .studyGoal(user.getStudyGoal())
                .profileCompleted(user.isProfileCompleted())
                .placementTestCompleted(placementTestAttemptRepository.existsByStudentAndTestCode(user, "IELTS_PLACEMENT_MOCK_1"))
                .build();
    }
}
