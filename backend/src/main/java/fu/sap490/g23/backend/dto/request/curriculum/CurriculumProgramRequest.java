package fu.sap490.g23.backend.dto.request.curriculum;

import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
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
public class CurriculumProgramRequest {
    @NotBlank(message = "Tên giáo trình không được để trống.")
    @Size(max = 180)
    private String title;

    @NotBlank(message = "Mã giáo trình không được để trống.")
    @Size(max = 120)
    private String code;

    @Size(max = 160)
    private String slug;

    @NotNull(message = "Hình thức đào tạo không được để trống.")
    private ClassroomDeliveryMode deliveryMode;

    @Size(max = 30)
    private String examCategory;

    @DecimalMin(value = "0.0", message = "Band mục tiêu không hợp lệ.")
    @DecimalMax(value = "9.0", message = "Band mục tiêu không hợp lệ.")
    private BigDecimal targetBand;

    @Min(0)
    private Integer targetScore;

    @Size(max = 120)
    private String entryLevel;

    private String outcomes;
    private String teacherGuide;
    private String interactionActivities;

    @Min(0)
    private Integer totalSessions;

    @Size(max = 30)
    private String status;

    @Min(0)
    private Integer displayOrder;
}
