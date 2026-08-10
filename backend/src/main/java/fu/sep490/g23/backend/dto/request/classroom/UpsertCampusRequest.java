package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertCampusRequest {
    @NotBlank(message = "Tên cơ sở không được để trống.")
    private String name;
    private String address;
    private String note;
    private Boolean active;
}
