package fu.sep490.g23.backend.service.auth_service;
import java.util.Set;

import fu.sep490.g23.backend.dto.request.LoginRequest;
import fu.sep490.g23.backend.dto.response.AuthResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.security.JwtService;
import fu.sep490.g23.backend.service.auth.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LoginTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private PlacementTestAttemptRepository placementTestAttemptRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private User validUser;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        validUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encoded_password")
                .fullName("Test User")
                .emailVerified(true)
                .roles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.LEARNER))
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setEmail("test@example.com");
        loginRequest.setPassword("password123");

        lenient().when(placementTestAttemptRepository.existsByStudentAndTestCode(any(User.class), eq("IELTS_PLACEMENT_CURRENT")))
                .thenReturn(false);
    }

    /**
     * Mục đích: Kiểm tra trường hợp đăng nhập thành công.
     * Kỳ vọng: Trả về thông tin user và JWT token hợp lệ.
     */
    @Test
    void login_Success_ReturnsAuthResponseWithToken() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(validUser));
        when(jwtService.generateToken(validUser)).thenReturn("mocked.jwt.token");
        // authenticationManager.authenticate() returns void or Authentication object, we just need it to not throw exception

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Đăng nhập thành công.", response.getMessage());
        assertEquals("mocked.jwt.token", response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals("test@example.com", response.getUser().getEmail());

        verify(authenticationManager, times(1)).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, times(1)).generateToken(validUser);
    }

    /**
     * Mục đích: Kiểm tra trường hợp đăng nhập với email không tồn tại trong hệ thống.
     * Kỳ vọng: Ném ra ngoại lệ báo lỗi sai email/mật khẩu.
     */
    @Test
    void login_Failure_EmailNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("notfound@example.com")).thenReturn(Optional.empty());
        loginRequest.setEmail("notfound@example.com");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("Email hoặc mật khẩu không đúng.", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
        verify(jwtService, never()).generateToken(any());
    }

    /**
     * Mục đích: Kiểm tra trường hợp tài khoản đã tồn tại nhưng chưa xác thực qua email.
     * Kỳ vọng: Ném ra ngoại lệ yêu cầu người dùng phải xác thực email trước.
     */
    @Test
    void login_Failure_EmailNotVerified_ThrowsException() {
        // Arrange
        validUser.setEmailVerified(false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(validUser));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });

        assertEquals("Tài khoản của bạn chưa xác thực email. Vui lòng kiểm tra hộp thư, nhập mã xác thực rồi thử lại.", exception.getMessage());
        verify(authenticationManager, never()).authenticate(any());
        verify(jwtService, never()).generateToken(any());
    }

    /**
     * Mục đích: Kiểm tra trường hợp người dùng nhập sai mật khẩu.
     * Kỳ vọng: AuthenticationManager sẽ ném ra BadCredentialsException.
     */
    @Test
    void login_Failure_BadCredentials_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(validUser));
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest);
        });

        verify(jwtService, never()).generateToken(any());
    }

    /**
     * Mục đích: Kiểm tra việc hệ thống có tự động cắt khoảng trắng (trim) và chuyển chữ hoa thành chữ thường (lowercase) cho email đầu vào hay không.
     * Kỳ vọng: Vẫn tìm thấy đúng user dù email có khoảng dư hoặc viết hoa.
     */
    @Test
    void login_TrimsAndLowercasesEmail() {
        // Arrange
        loginRequest.setEmail("  TEST@example.com  ");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(validUser));
        when(jwtService.generateToken(validUser)).thenReturn("token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }
}
