package fu.sep490.g23.backend.service.user;

import fu.sep490.g23.backend.dto.request.UpdateProfileRequest;
import fu.sep490.g23.backend.dto.request.ChangePasswordRequest;
import fu.sep490.g23.backend.dto.response.UserResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {

    UserResponse getCurrentUser(String email);

    UserResponse updateProfile(String email, UpdateProfileRequest request);

    UserResponse updateAvatar(String email, MultipartFile file, String publicUrlBase);

    UserResponse deleteAvatar(String email);

    void changePassword(String email, ChangePasswordRequest request);
}
