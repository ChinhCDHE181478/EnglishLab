package fu.sap490.g23.backend.dto.request.classroom;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransferStudentRequest {

    @NotNull(message = "Học viên không được để trống")
    private Long studentId;

    @NotNull(message = "Lớp đích không được để trống")
    private Long targetClassroomOfferingId;

    private String note;
}
