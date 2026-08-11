package fu.sep490.g23.backend.dto.request.course;

import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseDiscussionReactionRequest {
    @NotNull(message = "Vui lòng chọn cảm xúc.")
    private CourseDiscussionReactionType type;
}
