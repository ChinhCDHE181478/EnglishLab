package fu.sap490.g23.backend.dto.response.admin;

import fu.sap490.g23.backend.entity.enums.RoleEnum;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Builder
public class AdminUserResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phoneNumber;
    private Set<RoleEnum> roles;
    private boolean profileCompleted;
    private boolean emailVerified;
    private boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
