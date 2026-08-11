package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvailableTeacherOptionResponse {
    private Long id;
    private String fullName;
    private String email;
}
