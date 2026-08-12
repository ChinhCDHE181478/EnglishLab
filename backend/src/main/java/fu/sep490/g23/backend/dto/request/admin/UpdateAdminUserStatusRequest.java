package fu.sep490.g23.backend.dto.request.admin;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateAdminUserStatusRequest {
    @NotNull
    private Boolean enabled;
}
