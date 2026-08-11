package fu.sep490.g23.backend.dto.request.course;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DiscussionModerationActionRequest {
    @Size(max = 500, message = "Ghi chú xử lý không được vượt quá 500 ký tự.")
    private String actionNote;
}
