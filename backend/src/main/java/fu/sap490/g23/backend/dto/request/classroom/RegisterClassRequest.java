package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterClassRequest {
    /** true = Giữ chỗ, false = Đăng ký lớp */
    private Boolean holdSpot;

    private String note;
}
