package fu.sep490.g23.backend.dto.request.course;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
/** Course IDs used when appending to or reordering a learning path. */
public class LearningPathCoursesRequest {
    @NotEmpty
    private List<@NotNull Long> courseIds;
}
