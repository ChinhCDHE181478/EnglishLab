package fu.sep490.g23.backend.dto.request.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import jakarta.validation.constraints.NotNull;
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
public class ClassroomProposalScheduleItemRequest {
    private Integer sequenceNumber;

    @NotNull(message = "Ngày học không được để trống")
    private LocalDate sessionDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime endTime;

    private ClassroomDeliveryMode deliveryModeOverride;
    private Long teacherId;
    private Long roomId;
    private Long courseLessonId;
    private String sessionContent;
    private String note;
}
