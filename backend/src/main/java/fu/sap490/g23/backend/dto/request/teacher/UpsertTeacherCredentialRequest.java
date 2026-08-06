package fu.sap490.g23.backend.dto.request.teacher;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpsertTeacherCredentialRequest {

    @NotBlank(message = "Loại minh chứng là bắt buộc.")
    @Size(max = 40, message = "Loại minh chứng không được vượt quá 40 ký tự.")
    private String type;

    @NotBlank(message = "Tên chứng chỉ hoặc bằng cấp là bắt buộc.")
    @Size(max = 250, message = "Tên minh chứng không được vượt quá 250 ký tự.")
    private String title;

    @NotBlank(message = "Đơn vị cấp là bắt buộc.")
    @Size(max = 250, message = "Đơn vị cấp không được vượt quá 250 ký tự.")
    private String issuer;

    @Size(max = 150, message = "Mã chứng chỉ không được vượt quá 150 ký tự.")
    private String credentialNumber;

    private LocalDate issuedDate;
    private LocalDate expiryDate;

    @Size(max = 700, message = "Đường dẫn minh chứng không được vượt quá 700 ký tự.")
    private String documentUrl;
}
