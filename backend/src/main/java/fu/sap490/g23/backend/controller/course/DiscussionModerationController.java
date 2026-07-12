package fu.sap490.g23.backend.controller.course;

import fu.sap490.g23.backend.dto.request.course.DiscussionModerationActionRequest;
import fu.sap490.g23.backend.dto.response.course.DiscussionModerationReportResponse;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReportStatus;
import fu.sap490.g23.backend.service.course.DiscussionModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/content-manager/discussion-reports")
@RequiredArgsConstructor
public class DiscussionModerationController {
    private final DiscussionModerationService moderationService;

    @GetMapping
    public ResponseEntity<List<DiscussionModerationReportResponse>> getReports(
            @RequestParam(defaultValue = "PENDING") CourseDiscussionReportStatus status
    ) {
        return ResponseEntity.ok(moderationService.getReports(status));
    }

    @PostMapping("/{reportId}/hide")
    public ResponseEntity<DiscussionModerationReportResponse> hide(
            @PathVariable Long reportId,
            @Valid @RequestBody(required = false) DiscussionModerationActionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(moderationService.hide(reportId, request, authentication.getName()));
    }

    @PostMapping("/{reportId}/dismiss")
    public ResponseEntity<DiscussionModerationReportResponse> dismiss(
            @PathVariable Long reportId,
            @Valid @RequestBody(required = false) DiscussionModerationActionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(moderationService.dismiss(reportId, request, authentication.getName()));
    }
}
