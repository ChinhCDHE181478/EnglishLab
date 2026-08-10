package fu.sap490.g23.backend.dto.response.classroom;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ClassroomGradebookHomeworkResponse {
    private Long id;
    private String title;
    private BigDecimal score;
    private BigDecimal maxScore;
    private String status;
}
