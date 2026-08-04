package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.enums.LarkMeetingStatus;

/**
 * Provider-neutral gateway for virtual classroom meeting rooms.
 *
 * The persistence model still uses legacy lark_* columns for backward
 * compatibility. Provider-specific code must stay behind this interface.
 */
public interface VirtualMeetingService {

    String getPlatformName();

    boolean isEnabled();

    LarkMeetingStatus resolveStatus(String meetingUrl);

    boolean isJoinable(String meetingUrl, LarkMeetingStatus status);

    boolean isLegacyOrPlaceholderUrl(String meetingUrl);

    void syncMeeting(ClassroomSession session);

    void inviteInternalAttendee(ClassroomSession session, String email);

    void deleteMeeting(ClassroomSession session);
}
