package fu.sap490.g23.backend.dto.request.course;

import com.fasterxml.jackson.annotation.JsonAlias;
import fu.sap490.g23.backend.entity.course.CourseCategoryCode;
import fu.sap490.g23.backend.entity.course.CourseLevel;
import fu.sap490.g23.backend.entity.course.PackageStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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

    @NotNull(message = "Course category is required")
    private CourseCategoryCode category;

    @NotNull(message = "Course level is required")
    private CourseLevel level;

    private PackageStatus status;

    @Size(max = 80)
    private String targetScore;

    @Size(max = 80)
    private String duration;

    @Size(max = 120)
    private String studyMode;

    @DecimalMin(value = "0.00")
    private BigDecimal price;

    @Size(max = 700)
    private String thumbnailUrl;

    @Min(0)
    private Integer totalLessons;

    @Min(0)
    private Integer totalHours;

    @Min(0)
    private Integer displayOrder;

    private Boolean featured;

    @Valid
    @Builder.Default
    @JsonAlias("sections")
    private List<ModuleRequest> modules = new ArrayList<>();
}
