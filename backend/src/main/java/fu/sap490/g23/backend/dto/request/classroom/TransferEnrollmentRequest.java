package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferEnrollmentRequest {
    @NotNull(message = "Lớp đích không hợp lệ.")
    private Long targetClassroomOfferingId;

    private String note;
}
