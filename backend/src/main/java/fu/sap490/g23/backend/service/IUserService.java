package fu.sap490.g23.backend.service;

import fu.sap490.g23.backend.dto.response.UserResponse;

public interface IUserService {

    UserResponse getCurrentUser(String email);
}
