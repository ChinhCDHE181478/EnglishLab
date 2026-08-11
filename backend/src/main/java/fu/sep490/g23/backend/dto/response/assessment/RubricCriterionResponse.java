package fu.sep490.g23.backend.dto.response.assessment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RubricCriterionResponse {
    private Long id;
    private String name;
    private Integer weight;
    private String description;
    private String bandDescriptors;
    private Integer displayOrder;
}
