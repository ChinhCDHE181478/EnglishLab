package fu.sep490.g23.backend.service.user;

import fu.sep490.g23.backend.dto.request.ChangePasswordRequest;
import fu.sep490.g23.backend.dto.response.UserResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.service.user.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private PlacementTestAttemptRepository placementTestAttemptRepository;
    @Mock private AvatarStorageService avatarStorageService;
    @Mock private PasswordEncoder passwordEncoder;

    private UserServiceImpl service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(
                userRepository,
                placementTestAttemptRepository,
                avatarStorageService,
                passwordEncoder
        );
        user = User.builder()
                .id(10L)
                .fullName("Learner Test")
                .email("learner@test.vn")
                .password("encoded-current")
                .roles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.LEARNER))
                .emailVerified(true)
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
    }

    @Test
    void changePassword_WithCorrectCurrentPassword_EncodesAndSavesNewPassword() {
        ChangePasswordRequest request = passwordRequest("Current123!", "NextPassword123!", "NextPassword123!");
        when(passwordEncoder.matches("Current123!", "encoded-current")).thenReturn(true);
        when(passwordEncoder.matches("NextPassword123!", "encoded-current")).thenReturn(false);
        when(passwordEncoder.encode("NextPassword123!")).thenReturn("encoded-next");

        service.changePassword(user.getEmail(), request);

        assertEquals("encoded-next", user.getPassword());
        verify(userRepository).save(user);
    }

    @Test
    void changePassword_WithWrongCurrentPassword_RejectsWithoutSaving() {
        ChangePasswordRequest request = passwordRequest("Wrong123!", "NextPassword123!", "NextPassword123!");
        when(passwordEncoder.matches("Wrong123!", "encoded-current")).thenReturn(false);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.changePassword(user.getEmail(), request)
        );

        assertEquals("Mật khẩu hiện tại không đúng.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_WithMismatchedConfirmation_RejectsWithoutSaving() {
        ChangePasswordRequest request = passwordRequest("Current123!", "NextPassword123!", "Different123!");
        when(passwordEncoder.matches("Current123!", "encoded-current")).thenReturn(true);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.changePassword(user.getEmail(), request)
        );

        assertEquals("Mật khẩu xác nhận không khớp.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void changePassword_ForSocialAccount_AllowsInitialPasswordWithoutCurrentPassword() {
        user.setPasswordSet(false);
        ChangePasswordRequest request = passwordRequest(null, "NextPassword123!", "NextPassword123!");
        when(passwordEncoder.encode("NextPassword123!")).thenReturn("encoded-next");

        service.changePassword(user.getEmail(), request);

        assertEquals("encoded-next", user.getPassword());
        assertEquals(true, user.isPasswordSet());
        verify(userRepository).save(user);
    }

    @Test
    void updateAvatar_ReplacesStoredAvatarAndReturnsUpdatedUser() {
        user.setAvatarUrl("http://localhost:8080/api/user/avatars/avatar-old.png");
        MockMultipartFile file = new MockMultipartFile("file", "avatar.png", "image/png", new byte[]{1});
        when(avatarStorageService.store(file)).thenReturn("avatar-new.png");
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = service.updateAvatar(
                user.getEmail(),
                file,
                "http://localhost:8080/api/user/avatars/"
        );

        assertEquals("http://localhost:8080/api/user/avatars/avatar-new.png", response.getAvatarUrl());
        verify(avatarStorageService).deleteByUrl("http://localhost:8080/api/user/avatars/avatar-old.png");
    }

    @Test
    void deleteAvatar_ClearsDatabaseValueAndDeletesStoredFile() {
        String oldUrl = "http://localhost:8080/api/user/avatars/avatar-old.png";
        user.setAvatarUrl(oldUrl);
        when(userRepository.save(user)).thenReturn(user);

        UserResponse response = service.deleteAvatar(user.getEmail());

        assertNull(response.getAvatarUrl());
        verify(avatarStorageService).deleteByUrl(oldUrl);
    }

    private ChangePasswordRequest passwordRequest(String currentPassword, String newPassword, String confirmPassword) {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(currentPassword);
        request.setNewPassword(newPassword);
        request.setConfirmPassword(confirmPassword);
        return request;
    }
}
