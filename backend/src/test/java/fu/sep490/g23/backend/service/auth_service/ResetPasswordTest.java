package fu.sep490.g23.backend.service.auth_service;
import java.util.Set;

import fu.sep490.g23.backend.dto.request.ResetPasswordRequest;
import fu.sep490.g23.backend.dto.response.AuthResponse;
import fu.sep490.g23.backend.entity.AuthToken;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResetPasswordTest {

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
                .roles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.LEARNER))
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
