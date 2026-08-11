package fu.sep490.g23.backend.dto.response.course;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseCompletionResponse {
    private Long courseId;
    private Long enrollmentId;
    private String courseTitle;
    private String courseSlug;
    private Integer progressPercent;
    private Integer totalLessons;
    private Integer completedLessons;
    private Integer totalAssessments;
    private Integer completedAssessments;
    private boolean completedRequiredLessons;
    private boolean completedRequiredAssessments;
    private boolean eligibleForCertificate;
    private CourseCompletionStatus status;
    private String statusReason;
    private LocalDateTime completionDate;
    private Long latestLessonId;
    private String latestLessonTitle;
    private LocalDateTime latestLessonAccessedAt;
}
