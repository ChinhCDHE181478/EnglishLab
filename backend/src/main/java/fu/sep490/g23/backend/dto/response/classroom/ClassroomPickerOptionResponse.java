package fu.sep490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomPickerOptionResponse {
    private Long id;
    private String label;
    private Integer capacity;
}
