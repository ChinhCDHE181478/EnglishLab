package fu.sap490.g23.backend.service.impl;

import fu.sap490.g23.backend.entity.Role;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserRoleService {
    private final RoleRepository roleRepository;

    public void assignRole(User user, RoleEnum role) {
        if (user.hasRole(role)) {
            return;
        }
        Role roleEntity = roleRepository.findByCodeAndActiveTrue(role)
                .orElseThrow(() -> new IllegalStateException("Role chưa được cấu hình: " + role));
        user.getRoles().add(roleEntity);
    }

    public void ensureRole(User user, RoleEnum role) {
        assignRole(user, role);
    }

    public void replaceRoles(User user, RoleEnum role) {
        user.getRoles().clear();
        assignRole(user, role);
    }
}
