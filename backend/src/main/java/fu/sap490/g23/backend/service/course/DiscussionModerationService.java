package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.DiscussionModerationActionRequest;
import fu.sap490.g23.backend.dto.response.course.DiscussionModerationReportResponse;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReportStatus;

import java.util.List;

public interface DiscussionModerationService {
    List<DiscussionModerationReportResponse> getReports(CourseDiscussionReportStatus status);
    DiscussionModerationReportResponse hide(Long reportId, DiscussionModerationActionRequest request, String reviewerEmail);
    DiscussionModerationReportResponse dismiss(Long reportId, DiscussionModerationActionRequest request, String reviewerEmail);
}
