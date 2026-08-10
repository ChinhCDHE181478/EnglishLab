package fu.sap490.g23.backend.dto.request.course;

import fu.sap490.g23.backend.entity.course.enums.EnrollmentStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdatePackageEnrollmentRequest {
    @NotNull(message = "Trạng thái ghi danh không được để trống.")
    private EnrollmentStatus status;
}
