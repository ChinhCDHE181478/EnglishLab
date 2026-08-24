package fu.sep490.g23.backend.controller.course;

import fu.sep490.g23.backend.dto.request.course.DiscussionModerationActionRequest;
import fu.sep490.g23.backend.dto.response.course.DiscussionModerationReportResponse;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportStatus;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportReasonCategory;
import fu.sep490.g23.backend.service.course.DiscussionModerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

import java.util.List;

@RestController
@RequestMapping("/api/content-manager/discussion-reports")
@RequiredArgsConstructor
public class DiscussionModerationController {
    private final DiscussionModerationService moderationService;

    @GetMapping
    public ResponseEntity<List<DiscussionModerationReportResponse>> getReports(
            @RequestParam(defaultValue = "PENDING") CourseDiscussionReportStatus status,
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(moderationService.getReports(status, parseCategory(category)));
    }

    @GetMapping("/page")
    public ResponseEntity<Page<DiscussionModerationReportResponse>> getReportsPage(
            @RequestParam(defaultValue = "PENDING") CourseDiscussionReportStatus status,
            @RequestParam(required = false) String category,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(moderationService.getReports(status, parseCategory(category), pageable));
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

    private CourseDiscussionReportReasonCategory parseCategory(String category) {
        if (category == null || category.isBlank() || "ALL".equalsIgnoreCase(category)) {
            return null;
        }
        try {
            return CourseDiscussionReportReasonCategory.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Loại báo cáo không hợp lệ.");
        }
    }
}
