package fu.sap490.g23.backend.service.user;

import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.entity.Role;
import fu.sap490.g23.backend.entity.User;

public interface UserRoleService {

    void assignRole(User user, RoleEnum role);

    void ensureRole(User user, RoleEnum role);

    void replaceRoles(User user, RoleEnum role);
}
