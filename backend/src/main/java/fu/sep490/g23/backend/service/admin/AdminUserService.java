package fu.sep490.g23.backend.service.admin;
import fu.sep490.g23.backend.dto.request.admin.UpdateAdminUserStatusRequest;
import fu.sep490.g23.backend.dto.request.admin.UpdateUserRolesRequest;
import fu.sep490.g23.backend.dto.request.admin.UpsertAdminUserRequest;
import fu.sep490.g23.backend.dto.response.admin.AdminUserResponse;
import fu.sep490.g23.backend.dto.response.admin.AdminDashboardResponse;

import fu.sep490.g23.backend.dto.request.admin.*;
import fu.sep490.g23.backend.dto.response.admin.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminUserService {
    AdminDashboardResponse getDashboard();
    Page<AdminUserResponse> getUsers(String keyword, String role, Pageable pageable);
    AdminUserResponse createUser(UpsertAdminUserRequest request, String requesterEmail);
    AdminUserResponse updateUser(Long id, UpsertAdminUserRequest request, String requesterEmail);
    AdminUserResponse updateRoles(Long id, UpdateUserRolesRequest request, String requesterEmail);
    AdminUserResponse updateStatus(Long id, UpdateAdminUserStatusRequest request, String requesterEmail);
    void resendTeacherOnboardingEmail(Long id, String requesterEmail);
}
