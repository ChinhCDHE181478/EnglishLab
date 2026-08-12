package fu.sep490.g23.backend.dto.request.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateClassroomSessionRequest {

    @NotNull(message = "Ngày học không được để trống")
    private LocalDate sessionDate;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime startTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime endTime;

    private Long teacherId;
    private ClassroomSessionStatus status;
    private ClassroomDeliveryMode deliveryMode;
    private Long roomId;
    private Long curriculumSessionPlanId;

    @Size(max = 700, message = "Đường dẫn phòng học trực tuyến không được vượt quá 700 ký tự")
    private String larkMeetingUrl;

    @Size(max = 2000, message = "Nội dung buổi học không được vượt quá 2.000 ký tự")
    private String sessionContent;

    @Size(max = 2000, message = "Ghi chú buổi học không được vượt quá 2.000 ký tự")
    private String note;

    @AssertTrue(message = "Giờ kết thúc phải sau giờ bắt đầu")
    public boolean isTimeRangeValid() {
        return startTime == null || endTime == null || endTime.isAfter(startTime);
    }
}
