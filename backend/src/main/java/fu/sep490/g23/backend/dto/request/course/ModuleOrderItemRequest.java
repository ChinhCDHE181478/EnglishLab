package fu.sap490.g23.backend.dto.request.course;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleOrderItemRequest {
    @NotNull
    private Long moduleId;

    @NotNull
    @Min(1)
    private Integer orderIndex;
}
