package fu.sep490.g23.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FacebookAuthRequest {

    @NotBlank(message = "Thiếu access token Facebook")
    @Size(max = 10000, message = "Facebook access token không hợp lệ")
    private String accessToken;
}
