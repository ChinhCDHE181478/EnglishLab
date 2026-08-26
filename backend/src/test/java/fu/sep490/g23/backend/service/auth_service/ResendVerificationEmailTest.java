package fu.sep490.g23.backend.service.auth_service;
import java.util.Set;

import fu.sep490.g23.backend.dto.response.AuthResponse;
import fu.sep490.g23.backend.entity.AuthToken;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.service.auth.impl.AuthServiceImpl;
import fu.sep490.g23.backend.service.auth.AuthTokenService;
import fu.sep490.g23.backend.service.mail.AuthMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ResendVerificationEmailTest {

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

    private User unverifiedUser;

    @BeforeEach
    void setUp() {
        unverifiedUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .emailVerified(false)
                .roles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.LEARNER))
                .build();

        lenient().when(placementTestAttemptRepository.existsByStudentAndTestCode(any(User.class), eq("IELTS_PLACEMENT_CURRENT")))
                .thenReturn(false);
    }

    /**
     * Mục đích: Kiểm tra trường hợp gửi lại email xác thực thành công.
     * Kỳ vọng: Tạo ra token mới và gọi hàm gửi email.
     */
    @Test
    void resendVerificationEmail_Success_IssuesNewTokenAndSendsEmail() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));

        AuthToken mockToken = new AuthToken();
        mockToken.setToken("new123");
        when(authTokenService.issueEmailVerificationToken(unverifiedUser)).thenReturn(mockToken);

        // Act
        AuthResponse response = authService.resendVerificationEmail("test@example.com");

        // Assert
        assertNotNull(response);
        assertEquals("Đã gửi lại mã xác thực. Vui lòng kiểm tra hộp thư của bạn.", response.getMessage());

        verify(authTokenService, times(1)).issueEmailVerificationToken(unverifiedUser);
        verify(authMailService, times(1)).sendVerificationEmail(unverifiedUser, "new123");
    }

    /**
     * Mục đích: Kiểm tra trường hợp yêu cầu gửi lại email xác thực nhưng tài khoản đã xác thực rồi.
     * Kỳ vọng: Không gửi email mới, trả về thông báo đã xác thực.
     */
    @Test
    void resendVerificationEmail_Success_AlreadyVerified_ReturnsMessageWithoutSending() {
        // Arrange
        unverifiedUser.setEmailVerified(true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));

        // Act
        AuthResponse response = authService.resendVerificationEmail("test@example.com");

        // Assert
        assertNotNull(response);
        assertEquals("Email này đã được xác thực rồi.", response.getMessage());

        verify(authTokenService, never()).issueEmailVerificationToken(any());
        verify(authMailService, never()).sendVerificationEmail(any(), any());
    }

    /**
     * Mục đích: Kiểm tra trường hợp yêu cầu gửi lại email xác thực cho email không tồn tại.
     * Kỳ vọng: Ném ra ngoại lệ không tìm thấy tài khoản.
     */
    @Test
    void resendVerificationEmail_Failure_EmailNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.resendVerificationEmail("notfound@example.com");
        });

        assertEquals("Không tìm thấy tài khoản với email này.", exception.getMessage());
        verify(authMailService, never()).sendVerificationEmail(any(), any());
    }

    /**
     * Mục đích: Đảm bảo chuỗi email được chuẩn hóa (trim, lowercase) trước khi tìm kiếm.
     */
    @Test
    void resendVerificationEmail_TrimsAndLowercasesEmail() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(unverifiedUser));
        when(authTokenService.issueEmailVerificationToken(any())).thenReturn(new AuthToken());

        // Act
        authService.resendVerificationEmail("  TEST@example.com  ");

        // Assert
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }
}
