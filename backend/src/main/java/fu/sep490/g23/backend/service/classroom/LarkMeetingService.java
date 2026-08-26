package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;

public interface LarkMeetingService {
    String getPlatformName();

    boolean isEnabled();

    LarkMeetingStatus resolveStatus(String meetingUrl);

    boolean isJoinable(String meetingUrl, LarkMeetingStatus status);

    boolean isDemoUrl(String meetingUrl);

    void syncMeeting(ClassSchedule session);

    void inviteAttendee(ClassSchedule session, String email);

    void inviteInternalAttendee(ClassSchedule session, String email);

    void deleteMeeting(ClassSchedule session);

    LarkRecordingInfo getRecording(ClassSchedule session);
}
