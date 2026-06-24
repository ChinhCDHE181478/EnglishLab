package fu.sap490.g23.backend.service.auth_service;

import fu.sap490.g23.backend.dto.response.AuthResponse;
import fu.sap490.g23.backend.entity.AuthToken;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sap490.g23.backend.service.auth.impl.AuthServiceImpl;
import fu.sap490.g23.backend.service.auth.AuthTokenService;
import fu.sap490.g23.backend.service.mail.AuthMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ForgotPasswordTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private AuthMailService authMailService;

    @Mock
    private PlacementTestAttemptRepository placementTestAttemptRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .build();

        lenient().when(placementTestAttemptRepository.existsByStudentAndTestCode(any(User.class), eq("IELTS_PLACEMENT_MOCK_1")))
                .thenReturn(false);
    }

    /**
     * Mục đích: Kiểm tra trường hợp gửi OTP đổi mật khẩu khi email tồn tại trong hệ thống.
     * Kỳ vọng: Tạo ra mã OTP và gửi qua email cho người dùng.
     */
    @Test
    void forgotPassword_Success_EmailExists_IssuesTokenAndSendsEmail() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(existingUser));

        AuthToken mockToken = new AuthToken();
        mockToken.setToken("reset123");
        when(authTokenService.issuePasswordResetToken(existingUser)).thenReturn(mockToken);

        // Act
        AuthResponse response = authService.forgotPassword("test@example.com");

        // Assert
        assertNotNull(response);
        assertEquals("Nếu email tồn tại trong hệ thống, chúng tôi đã gửi mã OTP đặt lại mật khẩu.", response.getMessage());

        verify(authTokenService, times(1)).issuePasswordResetToken(existingUser);
        verify(authMailService, times(1)).sendPasswordResetEmail(existingUser, "reset123");
    }

    /**
     * Mục đích: Kiểm tra trường hợp gửi OTP nhưng email không tồn tại.
     * Kỳ vọng: Vẫn trả về thông báo thành công (để tránh lỗi bảo mật dò quét email) nhưng KHÔNG gửi email.
     */
    @Test
    void forgotPassword_Success_EmailDoesNotExist_ReturnsSameMessageWithoutSending() {
        // Arrange
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // Act
        AuthResponse response = authService.forgotPassword("notfound@example.com");

        // Assert
        assertNotNull(response);
        assertEquals("Nếu email tồn tại trong hệ thống, chúng tôi đã gửi mã OTP đặt lại mật khẩu.", response.getMessage());

        verify(authTokenService, never()).issuePasswordResetToken(any());
        verify(authMailService, never()).sendPasswordResetEmail(any(), any());
    }

    /**
     * Mục đích: Kiểm tra việc hệ thống có tự động làm sạch (trim, lowercase) đầu vào.
     * Kỳ vọng: Service dùng email đã làm sạch để truy vấn.
     */
    @Test
    void forgotPassword_TrimsAndLowercasesEmail() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());

        // Act
        authService.forgotPassword("  TEST@example.com  ");

        // Assert
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }
}
