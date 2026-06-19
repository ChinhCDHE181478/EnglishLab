package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.entity.classroom.LarkMeetingStatus;

public interface LarkMeetingService {
    String getPlatformName();

    LarkMeetingStatus resolveStatus(String meetingUrl);

    boolean isJoinable(String meetingUrl, LarkMeetingStatus status);
}
