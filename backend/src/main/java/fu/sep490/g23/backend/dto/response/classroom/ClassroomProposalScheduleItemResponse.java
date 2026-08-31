package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassroomProposalScheduleItemResponse {
    private Long id;
    private Integer sequenceNumber;
    private LocalDate sessionDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private ClassroomDeliveryMode deliveryModeOverride;
    private Long teacherId;
    private String teacherName;
    private Long roomId;
    private String roomName;
    private Long courseLessonId;
    private String courseLessonTitle;
    private Long courseUnitId;
    private String courseUnitTitle;
    private String sessionContent;
    private String note;
}
