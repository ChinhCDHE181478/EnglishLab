package fu.sap490.g23.backend.service.admin;

import fu.sap490.g23.backend.dto.request.admin.*;
import fu.sap490.g23.backend.dto.response.admin.*;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    AdminDashboardResponse getDashboard();
    Page<AdminUserResponse> getUsers(String keyword, RoleEnum role, Pageable pageable);
    AdminUserResponse createUser(UpsertAdminUserRequest request, String requesterEmail);
    AdminUserResponse updateUser(Long id, UpsertAdminUserRequest request, String requesterEmail);
    AdminUserResponse updateRoles(Long id, UpdateUserRolesRequest request, String requesterEmail);
    AdminUserResponse updateStatus(Long id, UpdateAdminUserStatusRequest request, String requesterEmail);
}
