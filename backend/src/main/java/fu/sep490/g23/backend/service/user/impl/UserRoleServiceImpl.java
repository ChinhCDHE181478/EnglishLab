package fu.sep490.g23.backend.service.user.impl;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.Role;
import fu.sep490.g23.backend.repository.RoleRepository;
import fu.sep490.g23.backend.service.user.UserRoleService;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {
    private final RoleRepository roleRepository;

    @Override
    public void assignRole(User user, String roleCode) {
        String normalizedCode = normalize(roleCode);
        if (user.hasRole(normalizedCode)) {
            return;
        }
        user.getRoles().add(requireRole(normalizedCode));
    }

    @Override
    public void ensureRole(User user, String roleCode) {
        assignRole(user, roleCode);
    }

    @Override
    public void replaceRoles(User user, String roleCode) {
        user.getRoles().clear();
        assignRole(user, roleCode);
    }

    @Override
    public void replaceRoles(User user, Collection<String> roleCodes) {
        if (roleCodes == null || roleCodes.isEmpty()) {
            throw new IllegalArgumentException("Người dùng phải có ít nhất một vai trò.");
        }
        LinkedHashSet<Role> resolvedRoles = new LinkedHashSet<>();
        roleCodes.stream().map(this::normalize).map(this::requireRole).forEach(resolvedRoles::add);
        user.getRoles().clear();
        user.getRoles().addAll(resolvedRoles);
    }

    private Role requireRole(String normalizedCode) {
        return roleRepository.findById(normalizedCode)
                .orElseThrow(() -> new IllegalArgumentException("Vai trò không tồn tại: " + normalizedCode));
    }

    private String normalize(String roleCode) {
        if (roleCode == null || roleCode.isBlank()) {
            throw new IllegalArgumentException("Mã vai trò không được để trống.");
        }
        return roleCode.trim().toUpperCase(Locale.ROOT);
    }
}
