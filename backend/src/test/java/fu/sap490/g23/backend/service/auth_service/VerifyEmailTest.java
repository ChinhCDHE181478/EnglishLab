package fu.sap490.g23.backend.service.auth_service;

import fu.sap490.g23.backend.dto.request.VerifyEmailRequest;
import fu.sap490.g23.backend.dto.response.AuthResponse;
import fu.sap490.g23.backend.entity.AuthToken;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sap490.g23.backend.service.auth.impl.AuthServiceImpl;
import fu.sap490.g23.backend.service.auth.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
                .role(RoleEnum.LEARNER)
                .build();

        validToken = new AuthToken();
        validToken.setToken("123456");
        validToken.setUser(unverifiedUser);

        lenient().when(placementTestAttemptRepository.existsByStudentAndTestCode(any(User.class), eq("IELTS_PLACEMENT_MOCK_1")))
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
}
