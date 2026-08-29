package fu.sep490.g23.backend.dto.request.classroom;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateClassroomPlanRequest {

    @Valid
    @NotNull
    private CreateClassroomOfferingRequest classroom;

    @Valid
    @NotEmpty(message = "Kế hoạch lớp phải có ít nhất một buổi học")
    @Builder.Default
    private List<ClassroomSchedulePlanItemRequest> schedules = new ArrayList<>();
}
