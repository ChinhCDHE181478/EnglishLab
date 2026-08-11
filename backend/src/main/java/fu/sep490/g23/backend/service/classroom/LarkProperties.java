package fu.sep490.g23.backend.service.classroom;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "englishlab.lark")
public class LarkProperties {

    private boolean enabled;
    private String appId;
    private String appSecret;
    private String calendarId;
    private String calendarName = "EnglishLab Virtual Training";
    private String timezone = "Asia/Ho_Chi_Minh";
    private String baseUrl = "https://open.larksuite.com/open-apis";
    private String verificationToken;
    private boolean autoRecord;
    private boolean recordingAutoPublish;
    private String defaultOwnerOpenId;
    private int recordingMaxSyncAttempts = 20;
    private long recordingSyncDelayMs = 60_000;
}
