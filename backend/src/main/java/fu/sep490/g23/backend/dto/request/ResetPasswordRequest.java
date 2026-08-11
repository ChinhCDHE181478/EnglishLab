package fu.sep490.g23.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank(message = "Email là bắt buộc")
    @Email(message = "Email không hợp lệ")
    @Size(max = 150, message = "Email không được vượt quá 150 ký tự")
    private String email;

    @NotBlank(message = "Mã OTP là bắt buộc")
    @Pattern(regexp = "^\\d{6}$", message = "Mã OTP phải gồm 6 chữ số")
    private String code;

    @NotBlank(message = "Mật khẩu mới là bắt buộc")
    @Size(min = 8, max = 255, message = "Mật khẩu phải từ 8 đến 255 ký tự")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,255}$",
            message = "Mật khẩu phải có ít nhất 1 chữ in hoa, 1 chữ in thường, 1 số và 1 ký tự đặc biệt"
    )
    private String newPassword;
}
