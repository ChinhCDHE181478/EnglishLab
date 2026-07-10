package fu.sap490.g23.backend.dto.request.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class ConflictCheckRequest {
    private Long classroomOfferingId;
    private Long sessionId;
    private Long teacherId;
    private Long roomId;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<Long> learnerIds;
    private Long excludeSessionId;
    private Long targetClassroomOfferingId;
    private ClassroomChangeRequestType requestType;
    private Boolean checkSessionLocked;
    @Builder.Default
    private Boolean checkCapacity = true;
    private String larkMeetingUrl;
}
