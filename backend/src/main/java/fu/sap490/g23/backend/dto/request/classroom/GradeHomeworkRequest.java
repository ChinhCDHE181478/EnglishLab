package fu.sap490.g23.backend.dto.request.classroom;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GradeHomeworkRequest {

    private BigDecimal score;
    private String teacherFeedback;
}
