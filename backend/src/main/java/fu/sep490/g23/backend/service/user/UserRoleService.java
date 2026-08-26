package fu.sep490.g23.backend.service.user;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.entity.User;
import java.util.Collection;

public interface UserRoleService {

    void assignRole(User user, RoleEnum role);

    void ensureRole(User user, RoleEnum role);

    void replaceRoles(User user, RoleEnum role);
    void replaceRoles(User user, Collection<RoleEnum> roles);
}
