package fu.sep490.g23.backend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GoogleAuthRequest {
    @Size(max = 10000, message = "Google ID token không hợp lệ")
    private String idToken;

    @Size(max = 10000, message = "Google access token không hợp lệ")
    private String accessToken;
}
