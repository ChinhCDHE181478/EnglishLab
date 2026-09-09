package fu.sep490.g23.backend.dto.response.curriculum;

import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class InstructorLedCourseResponse {
    private Long id;
    private String title;
    private String code;
    private String shortDescription;
    private String description;
    private String durationLabel;
    private String level;
    private BigDecimal baseTuitionFeeVnd;
    private BigDecimal saleTuitionFeeVnd;
    private String examCategory;
    private String focusSkills;
    private BigDecimal targetBand;
    private Integer targetScore;
    private String entryLevel;
    private PlacementLevel entryPlacementLevel;
    private String outcomes;
    private String teacherGuide;
    private String interactionActivities;
    private Integer totalSessions;
    private Integer totalLessons;
    private Integer totalUnits;
    private String status;
    private String statusLabel;
    private String reviewNote;
    private String submittedByName;
    private LocalDateTime submittedAt;
    private String reviewedByName;
    private LocalDateTime reviewedAt;
    /** Tổng số lớp học đã từng gắn giáo trình này. */
    private Integer classroomUsageCount;
    /** Số lớp sắp khai giảng / đang diễn ra dùng giáo trình này. */
    private Integer activeClassroomCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<CourseUnitResponse> units;
    /** Danh sách lớp đang gắn giáo trình (chỉ trả về ở API chi tiết). */
    private List<ClassroomUsage> usingClassrooms;

    @Data
    @Builder
    public static class ClassroomUsage {
        private Long id;
        private String title;
        private String status;
        private String statusLabel;
        private LocalDate startDate;
    }
}
