package fu.sap490.g23.backend.dto.request.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TrainingProgramRequest {
    @NotBlank(message = "Tên chương trình không được để trống")
    @Size(max = 180)
    private String title;

    @Size(max = 120)
    private String code;

    @Size(max = 160)
    private String slug;

    @NotNull(message = "Hình thức chương trình không được để trống")
    private ClassroomDeliveryMode deliveryMode;

    @NotNull(message = "Giáo trình không được để trống")
    private Long curriculumProgramId;

    @Size(max = 500)
    private String shortDescription;

    private String description;

    @Size(max = 120)
    private String entryLevel;

    @Size(max = 80)
    private String targetScore;

    @Size(max = 700)
    private String targetOutcome;

    @Min(1)
    private Integer defaultCapacity;

    private BigDecimal price;
    private BigDecimal salePrice;

    @Size(max = 80)
    private String duration;

    @Size(max = 120)
    private String studyMode;

    @Size(max = 700)
    private String thumbnailUrl;

    private String syllabusSummary;
    private String programOutcomes;
    private String teacherGuide;
    private String interactionActivities;
    private PackageStatus status;

    @Min(0)
    private Integer displayOrder;

    private Boolean featured;
    private List<Long> materialIds;
}
