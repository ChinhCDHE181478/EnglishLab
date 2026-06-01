package fu.sap490.g23.backend.dto.request.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModuleRequest {

    private Long id;

    @NotBlank(message = "Module title is required")
    @Size(max = 180)
    private String title;

    @Size(max = 500)
    private String description;

    @Min(0)
    private Integer displayOrder;

    @Valid
    @Builder.Default
    private List<LessonRequest> lessons = new ArrayList<>();
}
