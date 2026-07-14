package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.enums.LarkMeetingStatus;

public interface LarkMeetingService {
    String getPlatformName();

    boolean isEnabled();

    LarkMeetingStatus resolveStatus(String meetingUrl);

    boolean isJoinable(String meetingUrl, LarkMeetingStatus status);

    boolean isDemoUrl(String meetingUrl);

    void syncMeeting(ClassroomSession session);

    void inviteAttendee(ClassroomSession session, String email);

    void inviteInternalAttendee(ClassroomSession session, String email);

    void deleteMeeting(ClassroomSession session);

    LarkRecordingInfo getRecording(ClassroomSession session);
}
