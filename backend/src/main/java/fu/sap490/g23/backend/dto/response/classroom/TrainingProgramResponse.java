package fu.sap490.g23.backend.dto.response.classroom;

import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private Long curriculumProgramId;
    private String curriculumProgramTitle;
    private String curriculumProgramCode;
    private String curriculumProgramExamCategory;
    private String programTrack;
    private String focusSkills;
    private String curriculumProgramStatus;
    private String shortDescription;
    private String description;
    private String entryLevel;
    private String targetScore;
    private String targetOutcome;
    private BigDecimal price;
    private BigDecimal salePrice;
    private String duration;
    private String studyMode;
    private Integer capacity;
    /** Trường tương thích cho client cũ; dùng capacity cho tích hợp mới. */
    private Integer maxCapacity;
    private LocalDate plannedStartDate;
    private String plannedSchedule;
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
