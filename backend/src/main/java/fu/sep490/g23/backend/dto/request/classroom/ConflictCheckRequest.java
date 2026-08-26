package fu.sep490.g23.backend.dto.request.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.AssertTrue;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
public class ConflictCheckRequest {
    private Long classSectionId;
    private Long sessionId;
    private Long teacherId;
    private Long roomId;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private List<Long> learnerIds;
    private Long excludeSessionId;
    private Long targetClassSectionId;
    private ClassroomChangeRequestType requestType;
    private Boolean checkSessionLocked;
    @Builder.Default
    private Boolean checkCapacity = true;
    private String larkMeetingUrl;

    @AssertTrue(message = "Cần cung cấp đầy đủ ngày, giờ bắt đầu và giờ kết thúc khi kiểm tra lịch")
    public boolean isScheduleWindowComplete() {
        boolean anyProvided = sessionDate != null || startTime != null || endTime != null;
        return !anyProvided || (sessionDate != null && startTime != null && endTime != null);
    }

    @AssertTrue(message = "Giờ kết thúc phải sau giờ bắt đầu")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
