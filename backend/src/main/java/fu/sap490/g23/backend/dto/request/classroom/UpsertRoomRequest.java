package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertRoomRequest {
    @NotBlank(message = "Tên phòng không được để trống.")
    private String name;
    private Long campusId;
    private Integer capacity;
    private Boolean active;
}
