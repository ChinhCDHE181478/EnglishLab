package fu.sap490.g23.backend.service.auth_service;

import fu.sap490.g23.backend.dto.request.RegisterRequest;
import fu.sap490.g23.backend.dto.response.AuthResponse;
import fu.sap490.g23.backend.entity.AuthToken;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sap490.g23.backend.service.auth.impl.AuthServiceImpl;
import fu.sap490.g23.backend.service.auth.AuthTokenService;
import fu.sap490.g23.backend.service.user.UserRoleService;
import fu.sap490.g23.backend.service.mail.AuthMailService;
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
public class RegisterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private AuthMailService authMailService;

    @Mock
    private UserRoleService userRoleService;

    @Mock
    private PlacementTestAttemptRepository placementTestAttemptRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;
    private User existingUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setFullName("New User");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("StrongPass123!");

        existingUser = User.builder()
                .id(2L)
                .email("newuser@example.com")
                .emailVerified(true)
                .build();

        lenient().when(placementTestAttemptRepository.existsByStudentAndTestCode(any(User.class), eq("IELTS_PLACEMENT_CURRENT")))
                .thenReturn(false);
    }

    /**
     * Mục đích: Kiểm tra trường hợp đăng ký tài khoản mới hoàn toàn (email chưa từng tồn tại).
     * Kỳ vọng: Lưu user mới với role LEARNER, tạo mã xác thực và gửi email.
     */
    @Test
    void register_Success_NewUser_SendsEmailAndReturnsResponse() {
        // Arrange
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("StrongPass123!")).thenReturn("encoded_pass");

        User savedUser = User.builder()
                .id(1L)
                .email("newuser@example.com")
                .fullName("New User")
                .role(RoleEnum.LEARNER)
                .build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthToken mockToken = new AuthToken();
        mockToken.setToken("123456");
        when(authTokenService.issueEmailVerificationTokenForRegistration(savedUser)).thenReturn(mockToken);

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Đăng ký thành công. Vui lòng kiểm tra email và nhập mã xác thực để kích hoạt tài khoản.", response.getMessage());
        assertEquals("newuser@example.com", response.getUser().getEmail());

        verify(userRepository, times(1)).save(any(User.class));
        verify(userRoleService, times(1)).assignRole(any(User.class), eq(RoleEnum.LEARNER));
        verify(authMailService, times(1)).sendVerificationEmail(savedUser, "123456");
    }

    /**
     * Mục đích: Kiểm tra trường hợp đăng ký lại với email đã tồn tại nhưng CHƯA xác thực.
     * Kỳ vọng: Không ném lỗi trùng lặp mà sẽ cập nhật lại thông tin mới (mật khẩu, tên) và gửi lại email xác thực.
     */
    @Test
    void register_Success_ExistingUnverifiedUser_UpdatesAndResendsEmail() {
        // Arrange
        existingUser.setEmailVerified(false);
        existingUser.setRole(RoleEnum.LEARNER);

        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.encode("StrongPass123!")).thenReturn("encoded_pass_2");
        when(userRepository.save(existingUser)).thenReturn(existingUser);

        AuthToken mockToken = new AuthToken();
        mockToken.setToken("654321");
        when(authTokenService.issueEmailVerificationTokenForRegistration(existingUser)).thenReturn(mockToken);

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("Đăng ký thành công. Vui lòng kiểm tra email và nhập mã xác thực để kích hoạt tài khoản.", response.getMessage());


        assertEquals("New User", existingUser.getFullName());
        assertEquals("encoded_pass_2", existingUser.getPassword());

        verify(userRoleService, times(1)).replaceRoles(existingUser, RoleEnum.LEARNER);
        verify(userRepository, times(1)).save(existingUser);
        verify(authMailService, times(1)).sendVerificationEmail(existingUser, "654321");
    }

    /**
     * Mục đích: Kiểm tra trường hợp cố tình đăng ký với email ĐÃ TỒN TẠI và ĐÃ XÁC THỰC.
     * Kỳ vọng: Ném ra ngoại lệ ngăn chặn hành vi đăng ký trùng lặp.
     */
    @Test
    void register_Failure_EmailAlreadyRegisteredAndVerified_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.of(existingUser));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals("Email này đã được đăng ký.", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(authMailService, never()).sendVerificationEmail(any(), any());
    }

    /**
     * Mục đích: Kiểm tra việc hệ thống có tự động cắt khoảng trắng (trim) và chuyển chữ hoa thành chữ thường (lowercase) cho email và họ tên.
     * Kỳ vọng: Dữ liệu được lưu xuống database phải là dữ liệu đã được làm sạch.
     */
    @Test
    void register_TrimsAndLowercasesInput() {
        // Arrange
        registerRequest.setEmail("  NEWUSER@example.com  ");
        registerRequest.setFullName("  New User  ");
        when(userRepository.findByEmail("newuser@example.com")).thenReturn(Optional.empty());

        User savedUser = User.builder().id(1L).email("newuser@example.com").role(RoleEnum.LEARNER).build();
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(authTokenService.issueEmailVerificationTokenForRegistration(any())).thenReturn(new AuthToken());

        // Act
        authService.register(registerRequest);

        // Assert
        verify(userRepository, times(1)).findByEmail("newuser@example.com");
        // Verify the saved user has trimmed full name
        verify(userRepository).save(argThat(user -> user.getFullName().equals("New User") && user.getEmail().equals("newuser@example.com")));
    }
}
