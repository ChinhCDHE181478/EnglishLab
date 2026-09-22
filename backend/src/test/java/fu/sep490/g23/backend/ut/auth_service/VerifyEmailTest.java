package fu.sep490.g23.backend.ut.auth_service;

import fu.sep490.g23.backend.dto.request.ResetPasswordRequest;
import fu.sep490.g23.backend.dto.request.VerifyEmailRequest;
import fu.sep490.g23.backend.dto.response.AuthResponse;
import fu.sep490.g23.backend.entity.AuthToken;
import fu.sep490.g23.backend.entity.Role;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.service.auth.impl.AuthServiceImpl;
import fu.sep490.g23.backend.service.auth.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class VerifyEmailTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private PlacementTestAttemptRepository placementTestAttemptRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private VerifyEmailRequest verifyRequest;
    private User unverifiedUser;
    private AuthToken validToken;

    @BeforeEach
    void setUp() {
        verifyRequest = new VerifyEmailRequest();
        verifyRequest.setEmail("test@example.com");
        verifyRequest.setCode("123456");

        unverifiedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .emailVerified(false)
                .roles(Set.of(Role.builder().code(RoleCodes.LEARNER).displayName("Learner").active(true).build()))
                .build();

        validToken = new AuthToken();
        validToken.setToken("123456");
        validToken.setUser(unverifiedUser);

        lenient().when(placementTestAttemptRepository.existsByStudentAndTestCode(any(User.class), eq("IELTS_PLACEMENT_CURRENT")))
                .thenReturn(false);
    }

    /**
     * Mục đích: Kiểm tra trường hợp nhập mã xác thực email đúng.
     * Kỳ vọng: Cập nhật emailVerified thành true, đánh dấu mã OTP đã được dùng và trả về thông báo thành công.
     */
    @Test
    void verifyEmail_Success_MarksEmailAsVerified() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));
        when(authTokenService.requireValidEmailVerificationCode(unverifiedUser, "123456", "Mã xác thực không hợp lệ hoặc đã hết hạn."))
                .thenReturn(validToken);

        // Act
        AuthResponse response = authService.verifyEmail(verifyRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Xác thực email thành công. Bạn có thể đăng nhập ngay bây giờ.", response.getMessage());
        assertTrue(unverifiedUser.isEmailVerified());

        verify(userRepository, times(1)).save(unverifiedUser);
        verify(authTokenService, times(1)).markUsed(validToken);
    }

    /**
     * Mục đích: Kiểm tra trường hợp tài khoản ĐÃ XÁC THỰC RỒI nhưng vẫn gọi API xác thực lần nữa.
     * Kỳ vọng: Trả về thông báo thành công (đã xác thực rồi) mà không cần check lại OTP, không lưu DB thừa.
     */
    @Test
    void verifyEmail_Success_AlreadyVerified_ReturnsMessageWithoutUpdating() {
        // Arrange
        unverifiedUser.setEmailVerified(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));

        // Act
        AuthResponse response = authService.verifyEmail(verifyRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Email này đã được xác thực rồi.", response.getMessage());

        verify(authTokenService, never()).requireValidEmailVerificationCode(any(), any(), any());
        verify(userRepository, never()).save(any());
        verify(authTokenService, never()).markUsed(any());
    }

    /**
     * Mục đích: Kiểm tra trường hợp nhập email không có trong hệ thống để xác thực.
     * Kỳ vọng: Ném ngoại lệ báo không tìm thấy tài khoản.
     */
    @Test
    void verifyEmail_Failure_EmailNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
        verifyRequest.setEmail("notfound@example.com");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.verifyEmail(verifyRequest);
        });

        assertEquals("Không tìm thấy tài khoản với email này.", exception.getMessage());
    }

    /**
     * Mục đích: Kiểm tra trường hợp nhập sai mã OTP hoặc OTP hết hạn.
     * Kỳ vọng: Ném ngoại lệ báo OTP không hợp lệ, DB không bị update.
     */
    @Test
    void verifyEmail_Failure_InvalidCode_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));
        when(authTokenService.requireValidEmailVerificationCode(unverifiedUser, "123456", "Mã xác thực không hợp lệ hoặc đã hết hạn."))
                .thenThrow(new RuntimeException("Mã xác thực không hợp lệ hoặc đã hết hạn."));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.verifyEmail(verifyRequest);
        });

        assertEquals("Mã xác thực không hợp lệ hoặc đã hết hạn.", exception.getMessage());
        verify(userRepository, never()).save(any());
    }

    /**
     * Mục đích: Đảm bảo dữ liệu email được trim và lowercase trước khi tra cứu.
     */
    @Test
    void verifyEmail_TrimsAndLowercasesEmail() {
        // Arrange
        verifyRequest.setEmail("  TEST@example.com  ");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));
        when(authTokenService.requireValidEmailVerificationCode(any(), any(), any())).thenReturn(validToken);

        // Act
        authService.verifyEmail(verifyRequest);

        // Assert
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @ExtendWith(MockitoExtension.class)
    public static class ResetPasswordTest {

        @Mock
        private UserRepository userRepository;

        @Mock
        private AuthTokenService authTokenService;

        @Mock
        private PasswordEncoder passwordEncoder;

        @Mock
        private PlacementTestAttemptRepository placementTestAttemptRepository;

        @InjectMocks
        private AuthServiceImpl authService;

        private ResetPasswordRequest resetRequest;
        private User existingUser;
        private AuthToken validToken;

        @BeforeEach
        void setUp() {
            resetRequest = new ResetPasswordRequest();
            resetRequest.setEmail("test@example.com");
            resetRequest.setCode("123456");
            resetRequest.setNewPassword("NewStrongPass123!");

            existingUser = User.builder()
                    .id(1L)
                    .email("test@example.com")
                    .password("old_password")
                    .emailVerified(true)
                    .roles(Set.of(Role.builder().code(RoleCodes.LEARNER).displayName("Learner").active(true).build()))
                    .build();

            validToken = new AuthToken();
            validToken.setToken("123456");
            validToken.setUser(existingUser);

            lenient().when(placementTestAttemptRepository.existsByStudentAndTestCode(any(User.class), eq("IELTS_PLACEMENT_CURRENT")))
                    .thenReturn(false);
        }

        /**
         * Mục đích: Kiểm tra trường hợp đổi mật khẩu bằng OTP thành công.
         * Kỳ vọng: Cập nhật mật khẩu mới, đánh dấu email đã verify và đánh dấu OTP đã sử dụng.
         */
        @Test
        void resetPassword_Success_ChangesPasswordAndMarksTokenUsed() {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
            when(authTokenService.requireValidPasswordResetCode(existingUser, "123456", "Mã OTP không hợp lệ hoặc đã hết hạn."))
                    .thenReturn(validToken);
            when(passwordEncoder.encode("NewStrongPass123!")).thenReturn("encoded_new_password");

            // Act
            AuthResponse response = authService.resetPassword(resetRequest);

            // Assert
            assertNotNull(response);
            assertEquals("Đặt lại mật khẩu thành công. Bạn có thể đăng nhập với mật khẩu mới.", response.getMessage());
            assertEquals("test@example.com", response.getUser().getEmail());

            assertEquals("encoded_new_password", existingUser.getPassword());
            assertTrue(existingUser.isEmailVerified());

            verify(userRepository, times(1)).save(existingUser);
            verify(authTokenService, times(1)).markUsed(validToken);
        }

        /**
         * Mục đích: Kiểm tra trường hợp đổi mật khẩu nhưng email gửi lên không tồn tại.
         * Kỳ vọng: Ném lỗi OTP không hợp lệ để tránh lộ thông tin người dùng.
         */
        @Test
        void resetPassword_Failure_EmailNotFound_ThrowsException() {
            // Arrange
            when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
            resetRequest.setEmail("notfound@example.com");

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                authService.resetPassword(resetRequest);
            });

            assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn.", exception.getMessage());
            verify(authTokenService, never()).requireValidPasswordResetCode(any(), any(), any());
            verify(userRepository, never()).save(any());
        }

        /**
         * Mục đích: Kiểm tra trường hợp mã OTP gửi lên sai hoặc đã hết hạn.
         * Kỳ vọng: Ném lỗi OTP không hợp lệ, không thực hiện thay đổi mật khẩu.
         */
        @Test
        void resetPassword_Failure_InvalidOtp_ThrowsException() {
            // Arrange
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
            when(authTokenService.requireValidPasswordResetCode(existingUser, "123456", "Mã OTP không hợp lệ hoặc đã hết hạn."))
                    .thenThrow(new RuntimeException("Mã OTP không hợp lệ hoặc đã hết hạn."));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> {
                authService.resetPassword(resetRequest);
            });

            assertEquals("Mã OTP không hợp lệ hoặc đã hết hạn.", exception.getMessage());
            verify(passwordEncoder, never()).encode(any());
            verify(userRepository, never()).save(any());
            verify(authTokenService, never()).markUsed(any());
        }

        /**
         * Mục đích: Kiểm tra việc tự động làm sạch chuỗi đầu vào.
         * Kỳ vọng: Vẫn hoạt động đúng dù email có khoảng trắng.
         */
        @Test
        void resetPassword_TrimsAndLowercasesEmail() {
            // Arrange
            resetRequest.setEmail("  TEST@example.com  ");
            when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));
            when(authTokenService.requireValidPasswordResetCode(any(), any(), any())).thenReturn(validToken);
            when(passwordEncoder.encode(any())).thenReturn("encoded");

            // Act
            authService.resetPassword(resetRequest);

            // Assert
            verify(userRepository, times(1)).findByEmail("test@example.com");
        }
    }
}
