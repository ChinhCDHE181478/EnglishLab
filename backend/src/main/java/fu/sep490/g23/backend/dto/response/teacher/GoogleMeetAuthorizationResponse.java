package fu.sap490.g23.backend.dto.response.teacher;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GoogleMeetAuthorizationResponse {
    private String authorizationUrl;
}
