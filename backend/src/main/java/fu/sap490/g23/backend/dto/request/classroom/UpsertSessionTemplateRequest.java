package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertSessionTemplateRequest {
    @NotBlank(message = "Tên mẫu lịch không được để trống.")
    private String name;
    @NotBlank(message = "Cấu hình khung giờ không được để trống.")
    private String slotsJson;
    private String description;
    private Boolean active;
}
