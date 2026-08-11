package fu.sep490.g23.backend.dto.request.admin;

import fu.sep490.g23.backend.entity.enums.RoleEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpsertAdminBroadcastRequest {
    @NotBlank(message = "Tiêu đề không được để trống.")
    @Size(max = 180, message = "Tiêu đề không được vượt quá 180 ký tự.")
    private String title;

    @NotBlank(message = "Nội dung không được để trống.")
    @Size(max = 4000, message = "Nội dung không được vượt quá 4.000 ký tự.")
    private String message;

    private RoleEnum targetRole;

    @Size(max = 500, message = "Đường dẫn hành động không được vượt quá 500 ký tự.")
    private String actionPath;

    @NotNull(message = "Vui lòng chọn kênh thông báo trong ứng dụng.")
    private Boolean sendInApp;

    @NotNull(message = "Vui lòng chọn kênh email.")
    private Boolean sendEmail;
}
