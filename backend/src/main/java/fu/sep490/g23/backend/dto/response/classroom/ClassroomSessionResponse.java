package fu.sep490.g23.backend.dto.response.classroom;

import com.fasterxml.jackson.annotation.JsonProperty;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.LarkMeetingStatus;
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
    private ClassroomDeliveryMode deliveryMode;
    private String deliveryModeLabel;
    private Long roomId;
    private String roomName;
    private String offlineAddress;
    private String larkMeetingUrl;
    private String larkMeetingNo;
    private LarkMeetingStatus larkMeetingStatus;
    private boolean larkJoinable;
    private String larkPlatformName;
    private String larkSyncStatus;
    private String larkSyncError;
    private LocalDateTime larkSyncedAt;
    private String recordingUrl;
    private boolean recordingVisible;
    private RecordingSyncStatus recordingSyncStatus;
    private String recordingProvider;
    private Long recordingDurationMs;
    private LocalDateTime recordingSyncedAt;
    private LocalDateTime recordingLastAttemptAt;
    private String recordingSyncError;
    private Integer recordingSyncAttempts;
    private LocalDateTime recordingPublishedAt;
    private LocalDateTime recordingExpiresAt;
    private String sessionContent;
    @JsonProperty("curriculumSessionPlanId")
    private Long courseLessonId;
    private Integer sessionNumber;
    private String sessionPlanTitle;
    private String sessionPlanDescription;
    private String learningObjectives;
    @JsonProperty("curriculumUnitId")
    private Long courseUnitId;
    @JsonProperty("curriculumUnitDisplayOrder")
    private Integer courseUnitSequenceNumber;
    @JsonProperty("curriculumUnitTitle")
    private String courseUnitTitle;
    private String note;
    private boolean locked;
    private boolean rescheduled;
    private boolean cancelled;
}
