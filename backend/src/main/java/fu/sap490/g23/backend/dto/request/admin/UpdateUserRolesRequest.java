package fu.sap490.g23.backend.dto.request.admin;

import fu.sap490.g23.backend.entity.enums.RoleEnum;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateUserRolesRequest {
    @NotEmpty(message = "Người dùng phải có ít nhất một vai trò.")
    private Set<RoleEnum> roles;
}
