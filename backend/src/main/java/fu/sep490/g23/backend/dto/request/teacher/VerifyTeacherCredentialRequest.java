package fu.sep490.g23.backend.dto.request.teacher;

import fu.sep490.g23.backend.entity.teacher.enums.CredentialVerificationStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class VerifyTeacherCredentialRequest {

    @NotNull(message = "Trạng thái xác minh là bắt buộc.")
    private CredentialVerificationStatus status;

    @Size(max = 700, message = "Ghi chú xác minh không được vượt quá 700 ký tự.")
    private String note;
}
