package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpsertRoomRequest {
    @NotBlank(message = "Tên phòng không được để trống.")
    private String name;
    private String locationName;
    private String locationAddress;
    private Integer capacity;
    private Boolean active;
}
