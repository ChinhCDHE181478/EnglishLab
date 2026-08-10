package fu.sap490.g23.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {

    private String message;
    private String accessToken;
    @Builder.Default
    private String tokenType = "Bearer";
    private UserResponse user;
}
