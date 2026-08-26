package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.ClassSection;

/**
 * Manages the single Google Meet room owned by a virtual class section.
 */
public interface VirtualMeetingService {

    boolean isEnabled();

    boolean isGoogleMeetUrl(String meetingUrl);

    boolean isJoinable(ClassSection classSection);

    void syncMeeting(ClassSchedule session);

    void inviteInternalAttendee(ClassSchedule session, String email);

    void deleteMeeting(ClassSchedule session);

    VirtualMeetingRecordingInfo getRecording(ClassSchedule session);
}
