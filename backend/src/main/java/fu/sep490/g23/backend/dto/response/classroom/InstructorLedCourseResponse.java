package fu.sep490.g23.backend.dto.response.classroom;

import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class InstructorLedCourseResponse {
    private Long id;
    private String title;
    private String code;
    private String slug;
    private ClassroomDeliveryMode deliveryType;
    private ClassroomDeliveryMode deliveryMode;
    private String deliveryModeLabel;
    private Long instructorLedCourseId;
    private String instructorLedCourseTitle;
    private String instructorLedCourseCode;
    private String instructorLedCourseExamType;
    private String examType;
    private String examCategory;
    private String programTrack;
    private String focusSkills;
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
    private PackageStatus status;
    private String statusLabel;
    private Integer classroomCount;
    private Integer activeClassroomCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
