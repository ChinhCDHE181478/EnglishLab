package fu.sap490.g23.backend.dto.request.course;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
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
public class ReorderLessonsRequest {
    @NotEmpty
    @Valid
    @Builder.Default
    private List<LessonOrderItemRequest> items = new ArrayList<>();
}
