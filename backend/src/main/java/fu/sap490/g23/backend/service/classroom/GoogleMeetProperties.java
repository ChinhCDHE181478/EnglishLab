package fu.sap490.g23.backend.service.classroom;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "englishlab.google-meet")
public class GoogleMeetProperties {

    private boolean enabled;
    private String clientId;
    private String clientSecret;
    private String refreshToken;
    private String tokenUri = "https://oauth2.googleapis.com/token";
    private String apiBaseUrl = "https://meet.googleapis.com/v2";
}
