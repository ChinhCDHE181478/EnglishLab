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
    private String authorizationUri = "https://accounts.google.com/o/oauth2/v2/auth";
    private String tokenUri = "https://oauth2.googleapis.com/token";
    private String apiBaseUrl = "https://meet.googleapis.com/v2";
    private String redirectUri = "http://localhost:8080/api/teacher/google-meet/callback";
    private String frontendReturnUrl = "http://localhost:5173/teacher/professional-profile";
    private boolean autoRecording = true;
    private boolean recordingAutoPublish;
    private int recordingMaxSyncAttempts = 20;
    private long recordingSyncDelayMs = 60000;
}
