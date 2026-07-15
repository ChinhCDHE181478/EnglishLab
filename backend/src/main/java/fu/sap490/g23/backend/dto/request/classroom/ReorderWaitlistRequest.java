package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ReorderWaitlistRequest {

    @NotEmpty(message = "Danh sách thứ tự chờ không được để trống.")
    private List<@NotNull Long> enrollmentIds;
}
