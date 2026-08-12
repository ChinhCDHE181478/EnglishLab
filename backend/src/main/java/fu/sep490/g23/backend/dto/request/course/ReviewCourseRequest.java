package fu.sep490.g23.backend.dto.request.course;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewCourseRequest {
    @NotBlank(message = "Vui lòng nhập ghi chú duyệt.")
    private String reviewNote;
}
