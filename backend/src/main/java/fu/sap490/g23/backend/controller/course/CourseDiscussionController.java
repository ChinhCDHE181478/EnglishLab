package fu.sap490.g23.backend.controller.course;

import fu.sap490.g23.backend.dto.request.course.CourseDiscussionReplyRequest;
import fu.sap490.g23.backend.dto.request.course.CourseDiscussionReactionRequest;
import fu.sap490.g23.backend.dto.request.course.CourseDiscussionReportRequest;
import fu.sap490.g23.backend.dto.request.course.CourseDiscussionThreadRequest;
import fu.sap490.g23.backend.dto.response.ApiResponse;
import fu.sap490.g23.backend.dto.response.course.CourseDiscussionReactionResponse;
import fu.sap490.g23.backend.dto.response.course.CourseDiscussionReplyResponse;
import fu.sap490.g23.backend.dto.response.course.CourseDiscussionThreadResponse;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReportTarget;
import fu.sap490.g23.backend.service.course.CourseDiscussionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class CourseDiscussionController {

    private final CourseDiscussionService discussionService;

    @GetMapping("/api/online-courses/{courseId}/discussions")
    public ResponseEntity<List<CourseDiscussionThreadResponse>> getDiscussions(
            @PathVariable Long courseId,
            @RequestParam(defaultValue = "ALL") String filter,
            Authentication authentication
    ) {
        String email = authentication == null || authentication instanceof AnonymousAuthenticationToken
                ? null
                : authentication.getName();
        return ResponseEntity.ok(discussionService.getCourseDiscussions(courseId, filter, email));
    }

    @PostMapping("/api/student/online-courses/{courseId}/discussions")
    public ResponseEntity<CourseDiscussionThreadResponse> createThread(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseDiscussionThreadRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(discussionService.createThread(courseId, request, authentication.getName()));
    }

    @PostMapping("/api/student/online-courses/discussions/{threadId}/replies")
    public ResponseEntity<CourseDiscussionReplyResponse> createReply(
            @PathVariable Long threadId,
            @Valid @RequestBody CourseDiscussionReplyRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(discussionService.createReply(threadId, request, authentication.getName()));
    }

    @PostMapping("/api/student/online-courses/discussions/replies/{replyId}/helpful")
    public ResponseEntity<CourseDiscussionReplyResponse> toggleHelpful(
            @PathVariable Long replyId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(discussionService.toggleHelpful(replyId, authentication.getName()));
    }

    @PostMapping("/api/student/online-courses/discussions/{threadId}/reactions")
    public ResponseEntity<CourseDiscussionThreadResponse> toggleThreadReaction(
            @PathVariable Long threadId,
            @Valid @RequestBody CourseDiscussionReactionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(discussionService.toggleThreadReaction(threadId, request, authentication.getName()));
    }

    @PostMapping("/api/student/online-courses/discussions/replies/{replyId}/reactions")
    public ResponseEntity<CourseDiscussionReplyResponse> toggleReplyReaction(
            @PathVariable Long replyId,
            @Valid @RequestBody CourseDiscussionReactionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(discussionService.toggleReplyReaction(replyId, request, authentication.getName()));
    }

    @GetMapping("/api/online-courses/discussions/{threadId}/reactions")
    public ResponseEntity<List<CourseDiscussionReactionResponse>> getThreadReactions(
            @PathVariable Long threadId
    ) {
        return ResponseEntity.ok(discussionService.getThreadReactions(threadId));
    }

    @GetMapping("/api/online-courses/discussions/replies/{replyId}/reactions")
    public ResponseEntity<List<CourseDiscussionReactionResponse>> getReplyReactions(
            @PathVariable Long replyId
    ) {
        return ResponseEntity.ok(discussionService.getReplyReactions(replyId));
    }

    @PatchMapping("/api/student/online-courses/discussions/{threadId}/resolved")
    public ResponseEntity<CourseDiscussionThreadResponse> markResolved(
            @PathVariable Long threadId,
            @RequestParam(required = false) Long replyId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(discussionService.markResolved(threadId, replyId, authentication.getName()));
    }

    @PostMapping("/api/student/online-courses/discussions/{threadId}/reports")
    public ResponseEntity<ApiResponse> reportThread(
            @PathVariable Long threadId,
            @Valid @RequestBody(required = false) CourseDiscussionReportRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(discussionService.reportContent(CourseDiscussionReportTarget.THREAD, threadId, request, authentication.getName()));
    }

    @PostMapping("/api/student/online-courses/discussions/replies/{replyId}/reports")
    public ResponseEntity<ApiResponse> reportReply(
            @PathVariable Long replyId,
            @Valid @RequestBody(required = false) CourseDiscussionReportRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(discussionService.reportContent(CourseDiscussionReportTarget.REPLY, replyId, request, authentication.getName()));
    }
}
