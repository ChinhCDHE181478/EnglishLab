package fu.sap490.g23.backend.dto.request;

import lombok.Data;

@Data
public class GoogleAuthRequest {
    private String idToken;
    private String accessToken;
}
