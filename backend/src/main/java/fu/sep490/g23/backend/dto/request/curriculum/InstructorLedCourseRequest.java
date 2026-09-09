package fu.sep490.g23.backend.dto.request.curriculum;

import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class InstructorLedCourseRequest {
    @NotBlank(message = "Tên giáo trình không được để trống.")
    @Size(max = 180)
    private String title;

    @Size(max = 120)
    private String code;

    @Size(max = 500)
    private String shortDescription;

    private String description;

    @Size(max = 80)
    private String durationLabel;

    @Size(max = 120)
    private String level;

    @DecimalMin(value = "0.0", message = "Học phí gốc không được âm.")
    private BigDecimal baseTuitionFeeVnd;

    @DecimalMin(value = "0.0", message = "Học phí ưu đãi không được âm.")
    private BigDecimal saleTuitionFeeVnd;

    @Size(max = 30)
    private String examCategory;

    @Size(max = 240)
    private String focusSkills;

    @DecimalMin(value = "0.0", message = "Band mục tiêu không hợp lệ.")
    @DecimalMax(value = "9.0", message = "Band mục tiêu không hợp lệ.")
    private BigDecimal targetBand;

    @Min(0)
    private Integer targetScore;

    @Size(max = 120)
    private String entryLevel;

    private PlacementLevel entryPlacementLevel;

    private String outcomes;
    private String teacherGuide;
    private String interactionActivities;

    @Size(max = 30)
    private String status;

}
