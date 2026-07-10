package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.CourseDiscussionReactionRequest;
import fu.sap490.g23.backend.dto.request.course.CourseDiscussionReplyRequest;
import fu.sap490.g23.backend.dto.request.course.CourseDiscussionReportRequest;
import fu.sap490.g23.backend.dto.request.course.CourseDiscussionThreadRequest;
import fu.sap490.g23.backend.dto.response.ApiResponse;
import fu.sap490.g23.backend.dto.response.course.CourseDiscussionReactionResponse;
import fu.sap490.g23.backend.dto.response.course.CourseDiscussionReplyResponse;
import fu.sap490.g23.backend.dto.response.course.CourseDiscussionThreadResponse;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReportTarget;
import java.util.List;

public interface CourseDiscussionService {
    List<CourseDiscussionThreadResponse> getCourseDiscussions(Long courseId, String filter, String email);

    CourseDiscussionThreadResponse createThread(Long courseId, CourseDiscussionThreadRequest request, String email);

    CourseDiscussionReplyResponse createReply(Long threadId, CourseDiscussionReplyRequest request, String email);

    CourseDiscussionReplyResponse toggleHelpful(Long replyId, String email);

    CourseDiscussionThreadResponse toggleThreadReaction(Long threadId, CourseDiscussionReactionRequest request, String email);

    CourseDiscussionReplyResponse toggleReplyReaction(Long replyId, CourseDiscussionReactionRequest request, String email);

    List<CourseDiscussionReactionResponse> getThreadReactions(Long threadId);

    List<CourseDiscussionReactionResponse> getReplyReactions(Long replyId);

    CourseDiscussionThreadResponse markResolved(Long threadId, Long replyId, String email);

    ApiResponse reportContent(CourseDiscussionReportTarget targetType, Long targetId, CourseDiscussionReportRequest request, String email);
}
