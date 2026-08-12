package fu.sep490.g23.backend.dto.request.course;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateCourseVersionRequest {
    @Size(max = 700)
    private String changeNote;
}
