package fu.sep490.g23.backend.dto.request.classroom;

import com.fasterxml.jackson.annotation.JsonAlias;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TrainingProgramRequest {
    @NotBlank(message = "Tên khóa học theo lịch không được để trống")
    @Size(max = 180)
    private String title;

    @Size(max = 120)
    private String code;

    @Size(max = 160)
    private String slug;

    @JsonAlias("deliveryMode")
    @NotNull(message = "Hình thức khóa học không được để trống")
    private ClassroomDeliveryMode deliveryType;

    @NotNull(message = "Chương trình đào tạo không được để trống")
    private Long curriculumProgramId;

    @Size(max = 500)
    private String shortDescription;

    private String description;

    private BigDecimal price;
    private BigDecimal salePrice;

    @Size(max = 80)
    private String duration;

    @Size(max = 120)
    private String studyMode;

    @JsonAlias("maxCapacity")
    @Min(value = 1, message = "Sức chứa dự kiến phải lớn hơn 0")
    private Integer capacity;

    private LocalDate plannedStartDate;

    @Size(max = 500)
    private String plannedSchedule;

    @Size(max = 700)
    private String thumbnailUrl;

    private PackageStatus status;

    @Min(0)
    private Integer displayOrder;

    private Boolean featured;
}
