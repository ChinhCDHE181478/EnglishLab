package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.entity.classroom.LarkMeetingStatus;
import org.springframework.stereotype.Service;

@Service
public class ManualLarkMeetingService implements LarkMeetingService {

    @Override
    public String getPlatformName() {
        return "Lark";
    }

    @Override
    public LarkMeetingStatus resolveStatus(String meetingUrl) {
        if (meetingUrl == null || meetingUrl.isBlank()) {
            return LarkMeetingStatus.NOT_CREATED;
        }
        return LarkMeetingStatus.SCHEDULED;
    }

    @Override
    public boolean isJoinable(String meetingUrl, LarkMeetingStatus status) {
        return meetingUrl != null && !meetingUrl.isBlank()
                && (status == LarkMeetingStatus.OPEN || status == LarkMeetingStatus.IN_PROGRESS);
    }
}
