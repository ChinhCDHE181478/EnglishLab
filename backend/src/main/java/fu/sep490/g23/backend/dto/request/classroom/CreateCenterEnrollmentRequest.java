package fu.sep490.g23.backend.dto.request.classroom;

import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateCenterEnrollmentRequest {
    @NotBlank(message = "Vui lòng nhập họ và tên học viên")
    @Size(max = 100, message = "Họ và tên không được vượt quá 100 ký tự")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập email học viên")
    @Email(message = "Email học viên không hợp lệ")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
    private String email;

    @NotBlank(message = "Vui lòng nhập số điện thoại học viên")
    @Size(max = 30, message = "Số điện thoại không được vượt quá 30 ký tự")
    @Pattern(
            regexp = "^[0-9+() .-]{8,30}$",
            message = "Số điện thoại chỉ được chứa chữ số, dấu +, dấu chấm, dấu gạch ngang và khoảng trắng"
    )
    private String phoneNumber;

    @NotNull(message = "Vui lòng chọn trình độ đã xác nhận")
    private PlacementLevel confirmedLevel;

    @NotNull(message = "Vui lòng chọn lớp cho học viên")
    private Long classroomId;

    @Size(max = 700, message = "Ghi chú không được vượt quá 700 ký tự")
    private String note;
}
