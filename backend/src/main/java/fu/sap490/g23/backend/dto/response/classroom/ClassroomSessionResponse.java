package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.classroom.ClassroomSessionStatus;
import fu.sap490.g23.backend.entity.classroom.LarkMeetingStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class ClassroomSessionResponse {
    private Long id;
    private Long classroomOfferingId;
    private String classroomTitle;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long teacherId;
    private String teacherName;
    private ClassroomSessionStatus status;
    private ClassroomDeliveryMode deliveryMode;
    private String deliveryModeLabel;
    private Long campusId;
    private String campusName;
    private Long roomId;
    private String roomName;
    private String larkMeetingUrl;
    private LarkMeetingStatus larkMeetingStatus;
    private boolean larkJoinable;
    private String larkPlatformName;
    private String recordingUrl;
    private String sessionContent;
    private String note;
    private boolean locked;
    private boolean rescheduled;
    private boolean cancelled;
}
