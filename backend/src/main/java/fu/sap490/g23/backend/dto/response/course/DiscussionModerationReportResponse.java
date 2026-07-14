package fu.sap490.g23.backend.dto.response.course;

import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReportStatus;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReportTarget;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReportReasonCategory;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DiscussionModerationReportResponse {
    private Long reportId;
    private CourseDiscussionReportTarget targetType;
    private Long targetId;
    private CourseDiscussionReportReasonCategory reasonCategory;
    private String reason;
    private String reporterName;
    private String reporterEmail;
    private LocalDateTime createdAt;
    private Long courseId;
    private String courseTitle;
    private Long lessonId;
    private String lessonTitle;
    private String targetAuthor;
    private String contentPreview;
    private CourseDiscussionStatus currentTargetStatus;
    private int reportCount;
    private CourseDiscussionReportStatus status;
    private String reviewedBy;
    private LocalDateTime reviewedAt;
    private String actionNote;
}
