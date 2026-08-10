package fu.sap490.g23.backend.dto.request.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateChangeRequestRequest {

    @NotNull(message = "Loại yêu cầu không được để trống")
    private ClassroomChangeRequestType requestType;

    @NotNull(message = "Lớp học không được để trống")
    private Long classroomOfferingId;

    private Long targetSessionId;

    private String newValuesJson;

    @NotBlank(message = "Lý do không được để trống")
    private String reason;
}
