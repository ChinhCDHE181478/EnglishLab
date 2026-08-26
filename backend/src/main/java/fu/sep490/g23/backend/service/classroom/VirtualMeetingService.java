package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;

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

    void syncMeeting(ClassSchedule session);

    void inviteInternalAttendee(ClassSchedule session, String email);

    void deleteMeeting(ClassSchedule session);

    VirtualMeetingRecordingInfo getRecording(ClassSchedule session);
}
