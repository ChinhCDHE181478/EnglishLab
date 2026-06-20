package fu.sap490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClassroomSyllabusItemResponse {
    private Long id;
    private String title;
    private String description;
    private Integer displayOrder;
    private String sessionPlan;
    private String status;
}
