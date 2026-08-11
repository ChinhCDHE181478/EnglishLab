package fu.sep490.g23.backend.dto.request.course;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class LearningPathOrderRequest {
    @NotEmpty
    private List<@NotNull Long> courseIds;
}
