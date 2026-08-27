package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.GoogleMeetStatus;
import fu.sep490.g23.backend.entity.classroom.enums.RecordingSyncStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class ClassroomSessionResponse {
    private Long id;
    private Long classSectionId;
    private String classroomTitle;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private Long teacherId;
    private String teacherName;
    private ClassroomSessionStatus status;
    private ClassroomDeliveryMode deliveryModeOverride;
    private ClassroomDeliveryMode effectiveDeliveryMode;
    private String deliveryModeLabel;
    private Long roomId;
    private String roomName;
    private String offlineAddress;
    private String googleMeetUrl;
    private GoogleMeetStatus googleMeetStatus;
    private boolean googleMeetJoinable;
    private String recordingUrl;
    private boolean recordingVisible;
    private RecordingSyncStatus recordingStatus;
    private LocalDateTime recordingSyncedAt;
    private LocalDateTime recordingLastAttemptAt;
    private String recordingSyncError;
    private Integer recordingSyncAttempts;
    private String sessionContent;
    private Long courseLessonId;
    private Integer courseLessonSequenceNumber;
    private String courseLessonTitle;
    private String courseLessonDescription;
    private String learningObjectives;
    private Long courseUnitId;
    private Integer courseUnitSequenceNumber;
    private String courseUnitTitle;
    private String note;
    private boolean cancelled;
}
