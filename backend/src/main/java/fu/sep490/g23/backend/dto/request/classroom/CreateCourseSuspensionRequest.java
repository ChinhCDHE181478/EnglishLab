package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateCourseSuspensionRequest {

    @NotNull(message = "Vui lòng chọn lớp học cần bảo lưu.")
    private Long enrollmentId;

    @NotNull(message = "Vui lòng chọn ngày bắt đầu bảo lưu.")
    private LocalDate requestedStartDate;

    @NotNull(message = "Vui lòng chọn ngày dự kiến quay lại.")
    private LocalDate requestedReturnDate;

    @NotBlank(message = "Vui lòng nhập lý do bảo lưu.")
    @Size(max = 2000, message = "Lý do bảo lưu không được vượt quá 2000 ký tự.")
    private String reason;

    @NotBlank(message = "Vui lòng tải lên giấy tờ minh chứng.")
    @Size(max = 700, message = "Đường dẫn giấy tờ minh chứng không hợp lệ.")
    private String proofUrl;
}
