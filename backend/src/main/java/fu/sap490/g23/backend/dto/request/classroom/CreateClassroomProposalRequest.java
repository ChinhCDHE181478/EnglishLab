package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
public class CreateClassroomProposalRequest {
    @NotBlank(message = "Tên đề xuất lớp không được để trống")
    @Size(max = 180)
    private String title;

    @NotNull(message = "Khóa học không được để trống")
    private Long courseOfferingId;

    /** Trường tương thích dữ liệu cũ; đề xuất mở lớp không phụ thuộc danh sách học viên. */
    private List<Long> enrollmentRequestIds = List.of();

    @Min(value = 1, message = "Sức chứa phải lớn hơn 0")
    private Integer capacity;

    @NotNull(message = "Ngày bắt đầu không được để trống")
    private LocalDate plannedStartDate;

    @NotNull(message = "Ngày kết thúc không được để trống")
    private LocalDate plannedEndDate;

    @jakarta.validation.constraints.NotEmpty(message = "Cần chọn ít nhất một ngày học trong tuần")
    private List<DayOfWeek> weekdays;

    @NotNull(message = "Giờ bắt đầu không được để trống")
    private LocalTime sessionStartTime;

    @NotNull(message = "Giờ kết thúc không được để trống")
    private LocalTime sessionEndTime;

    private Long primaryTeacherId;
    private Long roomId;

    @Size(max = 500)
    private String offlineAddress;

    @Size(max = 700)
    private String virtualMeetingUrl;

    @Size(max = 700)
    private String note;
}
