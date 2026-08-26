package fu.sep490.g23.backend.dto.request.admin;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateUserRolesRequest {
    @NotEmpty(message = "Người dùng phải có ít nhất một vai trò.")
    private Set<String> roles;
}
