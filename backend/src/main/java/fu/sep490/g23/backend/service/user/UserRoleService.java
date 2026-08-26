package fu.sep490.g23.backend.service.user;
import fu.sep490.g23.backend.entity.User;
import java.util.Collection;

public interface UserRoleService {

    void assignRole(User user, String roleCode);

    void ensureRole(User user, String roleCode);

    void replaceRoles(User user, String roleCode);

    void replaceRoles(User user, Collection<String> roleCodes);
}
