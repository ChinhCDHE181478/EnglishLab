package fu.sap490.g23.backend.dto.response.teacher;

import fu.sap490.g23.backend.entity.teacher.enums.GoogleMeetConnectionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TeacherGoogleMeetConnectionResponse {
    private boolean connected;
    private boolean integrationEnabled;
    private GoogleMeetConnectionStatus status;
    private String googleEmail;
    private LocalDateTime connectedAt;
    private LocalDateTime lastUsedAt;
}
