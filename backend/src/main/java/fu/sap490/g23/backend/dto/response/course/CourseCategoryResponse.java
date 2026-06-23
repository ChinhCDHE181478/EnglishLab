package fu.sap490.g23.backend.dto.response.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseCategoryResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer displayOrder;
    private boolean active;
    private long courseCount;
}
