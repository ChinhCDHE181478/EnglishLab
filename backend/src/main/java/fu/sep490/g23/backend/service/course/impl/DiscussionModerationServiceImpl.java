package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.dto.request.course.DiscussionModerationActionRequest;
import fu.sep490.g23.backend.dto.response.course.DiscussionModerationReportResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.CourseDiscussionReply;
import fu.sep490.g23.backend.entity.course.CourseDiscussionReport;
import fu.sep490.g23.backend.entity.course.CourseDiscussionThread;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportStatus;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportReasonCategory;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionReportTarget;
import fu.sep490.g23.backend.entity.course.enums.CourseDiscussionStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReplyRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionReportRepository;
import fu.sep490.g23.backend.repository.course.CourseDiscussionThreadRepository;
import fu.sep490.g23.backend.service.course.DiscussionModerationService;
import fu.sep490.g23.backend.service.admin.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DiscussionModerationServiceImpl implements DiscussionModerationService {
    private final CourseDiscussionReportRepository reportRepository;
    private final CourseDiscussionThreadRepository threadRepository;
    private final CourseDiscussionReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Override
    @Transactional(readOnly = true)
    public List<DiscussionModerationReportResponse> getReports(CourseDiscussionReportStatus status, CourseDiscussionReportReasonCategory category) {
        CourseDiscussionReportStatus resolvedStatus = status == null ? CourseDiscussionReportStatus.PENDING : status;
        List<CourseDiscussionReport> reports = category == null
                ? reportRepository.findByStatusOrderByCreatedAtDesc(resolvedStatus)
                : reportRepository.findByStatusAndReasonCategoryOrderByCreatedAtDesc(resolvedStatus, category);
        return reports.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DiscussionModerationReportResponse> getReports(
            CourseDiscussionReportStatus status,
            CourseDiscussionReportReasonCategory category,
            Pageable pageable
    ) {
        CourseDiscussionReportStatus resolvedStatus = status == null
                ? CourseDiscussionReportStatus.PENDING
                : status;
        Page<CourseDiscussionReport> reports = category == null
                ? reportRepository.findByStatus(resolvedStatus, pageable)
                : reportRepository.findByStatusAndReasonCategory(resolvedStatus, category, pageable);
        return reports.map(this::toResponse);
    }

    @Override
    public DiscussionModerationReportResponse hide(Long reportId, DiscussionModerationActionRequest request, String reviewerEmail) {
        CourseDiscussionReport report = findActionableReport(reportId, CourseDiscussionReportStatus.PENDING, CourseDiscussionReportStatus.DISMISSED);
        if (report.getTargetType() == CourseDiscussionReportTarget.THREAD) {
            findThread(report.getTargetId()).setStatus(CourseDiscussionStatus.HIDDEN);
        } else {
            findReply(report.getTargetId()).setStatus(CourseDiscussionStatus.HIDDEN);
        }
        review(report, CourseDiscussionReportStatus.ACTION_TAKEN, request, reviewerEmail);
        auditLogService.record(reviewerEmail,"DISCUSSION_CONTENT_HIDDEN",report.getTargetType().name(),report.getTargetId().toString(),"Ẩn nội dung từ báo cáo #"+reportId);
        return toResponse(report);
    }

    @Override
    public DiscussionModerationReportResponse dismiss(Long reportId, DiscussionModerationActionRequest request, String reviewerEmail) {
        CourseDiscussionReport report = findActionableReport(reportId, CourseDiscussionReportStatus.PENDING, CourseDiscussionReportStatus.ACTION_TAKEN);
        if (report.getStatus() == CourseDiscussionReportStatus.ACTION_TAKEN) {
            if (report.getTargetType() == CourseDiscussionReportTarget.THREAD) {
                findThread(report.getTargetId()).setStatus(CourseDiscussionStatus.OPEN);
            } else {
                findReply(report.getTargetId()).setStatus(CourseDiscussionStatus.OPEN);
            }
        }
        review(report, CourseDiscussionReportStatus.DISMISSED, request, reviewerEmail);
        auditLogService.record(reviewerEmail,"DISCUSSION_REPORT_DISMISSED",report.getTargetType().name(),report.getTargetId().toString(),"Bỏ qua báo cáo #"+reportId);
        return toResponse(report);
    }

    private void review(CourseDiscussionReport report, CourseDiscussionReportStatus status, DiscussionModerationActionRequest request, String reviewerEmail) {
        User reviewer = userRepository.findByEmail(reviewerEmail).orElseThrow(() -> new RuntimeException("Không tìm thấy người kiểm duyệt."));
        report.setStatus(status);
        report.setReviewedBy(reviewer);
        report.setReviewedAt(LocalDateTime.now());
        report.setActionNote(request == null || request.getActionNote() == null ? null : request.getActionNote().trim());
        reportRepository.save(report);
    }

    private CourseDiscussionReport findActionableReport(Long reportId, CourseDiscussionReportStatus... allowedStatuses) {
        CourseDiscussionReport report = reportRepository.findById(reportId).orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo."));
        boolean allowed = java.util.Arrays.stream(allowedStatuses).anyMatch(status -> status == report.getStatus());
        if (!allowed) {
            throw new RuntimeException("Báo cáo này đã được xử lý.");
        }
        return report;
    }

    private CourseDiscussionThread findThread(Long id) {
        return threadRepository.findById(id).orElseThrow(() -> new RuntimeException("Nội dung thảo luận không còn tồn tại."));
    }

    private CourseDiscussionReply findReply(Long id) {
        return replyRepository.findById(id).orElseThrow(() -> new RuntimeException("Câu trả lời không còn tồn tại."));
    }

    private DiscussionModerationReportResponse toResponse(CourseDiscussionReport report) {
        CourseDiscussionThread thread;
        String content;
        String author;
        CourseDiscussionStatus targetStatus;
        int reportCount;
        if (report.getTargetType() == CourseDiscussionReportTarget.THREAD) {
            thread = findThread(report.getTargetId());
            content = thread.getTitle() + " — " + thread.getContent();
            author = displayName(thread.getAuthor());
            targetStatus = thread.getStatus();
            reportCount = thread.getReportedCount();
        } else {
            CourseDiscussionReply reply = findReply(report.getTargetId());
            thread = reply.getThread();
            content = reply.getContent();
            author = displayName(reply.getAuthor());
            targetStatus = reply.getStatus();
            reportCount = reply.getReportedCount();
        }
        return DiscussionModerationReportResponse.builder()
                .reportId(report.getId())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reasonCategory(report.getReasonCategory())
                .reason(report.getReason())
                .reporterName(displayName(report.getReporter()))
                .reporterEmail(report.getReporter().getEmail())
                .createdAt(report.getCreatedAt())
                .courseId(thread.getCourse().getId())
                .courseTitle(thread.getCourse().getLearningPackage().getTitle())
                .lessonId(thread.getLesson() == null ? null : thread.getLesson().getId())
                .lessonTitle(thread.getLesson() == null ? null : thread.getLesson().getTitle())
                .targetAuthor(author)
                .contentPreview(preview(content))
                .currentTargetStatus(targetStatus)
                .reportCount(reportCount)
                .status(report.getStatus())
                .reviewedBy(report.getReviewedBy() == null ? null : displayName(report.getReviewedBy()))
                .reviewedAt(report.getReviewedAt())
                .actionNote(report.getActionNote())
                .build();
    }

    private String displayName(User user) {
        return user.getFullName() == null || user.getFullName().isBlank() ? user.getEmail() : user.getFullName();
    }

    private String preview(String content) {
        if (content == null) return "";
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 300 ? normalized : normalized.substring(0, 297) + "...";
    }
}
