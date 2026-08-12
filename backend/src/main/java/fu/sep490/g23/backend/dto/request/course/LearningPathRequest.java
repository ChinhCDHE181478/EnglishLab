package fu.sep490.g23.backend.dto.request.course;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

@Data
public class LearningPathRequest {
    @NotBlank
    @Size(max = 80)
    private String code;

    @NotBlank
    @Size(max = 180)
    private String name;

    @Size(max = 30)
    private String examCategory;

    @DecimalMin("0.0")
    @DecimalMax("9.0")
    private BigDecimal targetBand;

    @Min(10)
    @Max(990)
    private Integer targetScore;
}
