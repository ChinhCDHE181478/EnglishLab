package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.request.course.DiscussionModerationActionRequest;
import fu.sep490.g23.backend.dto.response.course.DiscussionModerationReportResponse;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportStatus;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportReasonCategory;

import java.util.List;

public interface DiscussionModerationService {
    List<DiscussionModerationReportResponse> getReports(CourseDiscussionReportStatus status, CourseDiscussionReportReasonCategory category);
    DiscussionModerationReportResponse hide(Long reportId, DiscussionModerationActionRequest request, String reviewerEmail);
    DiscussionModerationReportResponse dismiss(Long reportId, DiscussionModerationActionRequest request, String reviewerEmail);
}
