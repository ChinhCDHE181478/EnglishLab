package fu.sap490.g23.backend.dto.response.course;

import fu.sap490.g23.backend.entity.course.enums.EnrollmentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PackageEnrollmentResponse {
    private Long id;
    private Long packageId;
    private Long courseId;
    private Long courseVersionId;
    private Integer courseVersionNumber;
    private String courseTitle;
    private String courseSlug;
    private String thumbnailUrl;
    private EnrollmentStatus status;
    private Integer progressPercent;
    private Integer streakDays;
    private LocalDateTime registeredAt;

    @Builder.Default
    private List<Long> completedLessonIds = new ArrayList<>();
}
