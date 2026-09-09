package fu.sep490.g23.backend.dto.request.course;

import com.fasterxml.jackson.annotation.JsonAlias;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnlineCourseRequest {

    @NotBlank(message = "Course title is required")
    @Size(max = 180)
    private String title;

    @Size(max = 500)
    private String shortDescription;

    private String description;

    @NotBlank(message = "Course category is required")
    @Size(max = 40)
    private String category;

    @NotNull(message = "Course level is required")
    private CourseLevel level;

    private PackageStatus status;

    @Size(max = 80)
    private String targetScore;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "9.0")
    private Double recommendedCurrentBandMin;

    @DecimalMin(value = "0.0")
    @DecimalMax(value = "9.0")
    private Double targetBand;

    @Size(max = 700)
    private String targetOutcome;

    @Size(max = 80)
    private String duration;

    @DecimalMin(value = "0.00")
    private BigDecimal price;

    @DecimalMin(value = "0.00")
    private BigDecimal salePrice;

    @Size(max = 700)
    private String thumbnailUrl;

    private Boolean featured;

    @Valid
    @Builder.Default
    @JsonAlias("sections")
    private List<ModuleRequest> modules = new ArrayList<>();
}
