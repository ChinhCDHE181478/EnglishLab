package fu.sap490.g23.backend.dto.request.admin;

import fu.sap490.g23.backend.entity.enums.RoleEnum;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpsertAdminUserRequest {
    @NotBlank @Size(max = 100)
    private String fullName;
    @NotBlank @Email @Size(max = 150)
    private String email;
    @Size(max = 30)
    private String phoneNumber;
    @Size(min = 6, max = 100)
    private String password;
    private Set<RoleEnum> roles;
}
