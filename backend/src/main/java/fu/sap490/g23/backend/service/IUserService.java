package fu.sap490.g23.backend.service;

import fu.sap490.g23.backend.dto.request.UpdateProfileRequest;
import fu.sap490.g23.backend.dto.response.UserResponse;

public interface IUserService {

    UserResponse getCurrentUser(String email);

    UserResponse updateProfile(String email, UpdateProfileRequest request);
}
