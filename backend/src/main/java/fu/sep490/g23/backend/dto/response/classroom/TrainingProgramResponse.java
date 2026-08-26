package fu.sep490.g23.backend.dto.response.classroom;

import com.fasterxml.jackson.annotation.JsonProperty;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TrainingProgramResponse {
    private Long id;
    private String title;
    private String code;
    private String slug;
    /** Tên canonical trên API; deliveryMode được giữ trong compatibility window. */
    private ClassroomDeliveryMode deliveryType;
    private ClassroomDeliveryMode deliveryMode;
    private String deliveryModeLabel;
    @JsonProperty("curriculumProgramId")
    private Long instructorLedCourseId;
    @JsonProperty("curriculumProgramTitle")
    private String instructorLedCourseTitle;
    @JsonProperty("curriculumProgramCode")
    private String instructorLedCourseCode;
    @JsonProperty("curriculumProgramExamCategory")
    private String instructorLedCourseExamType;
    private String programTrack;
    private String focusSkills;
    @JsonProperty("curriculumProgramStatus")
    private String instructorLedCourseStatus;
    private String shortDescription;
    private String description;
    private String entryLevel;
    private String targetScore;
    private String targetOutcome;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String duration;
    private String studyMode;
    private String thumbnailUrl;
    private PackageStatus status;
    private String statusLabel;
    private Integer displayOrder;
    private boolean featured;
    private Integer classroomCount;
    private Integer activeClassroomCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
