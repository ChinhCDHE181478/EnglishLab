package fu.sap490.g23.backend.service.course.impl;

import fu.sap490.g23.backend.dto.request.course.DiscussionModerationActionRequest;
import fu.sap490.g23.backend.dto.response.course.DiscussionModerationReportResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.CourseDiscussionReply;
import fu.sap490.g23.backend.entity.course.CourseDiscussionReport;
import fu.sap490.g23.backend.entity.course.CourseDiscussionThread;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReportStatus;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionReportTarget;
import fu.sap490.g23.backend.entity.course.enums.CourseDiscussionStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.CourseDiscussionReplyRepository;
import fu.sap490.g23.backend.repository.course.CourseDiscussionReportRepository;
import fu.sap490.g23.backend.repository.course.CourseDiscussionThreadRepository;
import fu.sap490.g23.backend.service.course.DiscussionModerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
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

    @Override
    @Transactional(readOnly = true)
    public List<DiscussionModerationReportResponse> getReports(CourseDiscussionReportStatus status) {
        CourseDiscussionReportStatus resolvedStatus = status == null ? CourseDiscussionReportStatus.PENDING : status;
        return reportRepository.findByStatusOrderByCreatedAtDesc(resolvedStatus).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public DiscussionModerationReportResponse hide(Long reportId, DiscussionModerationActionRequest request, String reviewerEmail) {
        CourseDiscussionReport report = findPendingReport(reportId);
        if (report.getTargetType() == CourseDiscussionReportTarget.THREAD) {
            findThread(report.getTargetId()).setStatus(CourseDiscussionStatus.HIDDEN);
        } else {
            findReply(report.getTargetId()).setStatus(CourseDiscussionStatus.HIDDEN);
        }
        review(report, CourseDiscussionReportStatus.ACTION_TAKEN, request, reviewerEmail);
        return toResponse(report);
    }

    @Override
    public DiscussionModerationReportResponse dismiss(Long reportId, DiscussionModerationActionRequest request, String reviewerEmail) {
        CourseDiscussionReport report = findPendingReport(reportId);
        review(report, CourseDiscussionReportStatus.DISMISSED, request, reviewerEmail);
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

    private CourseDiscussionReport findPendingReport(Long reportId) {
        CourseDiscussionReport report = reportRepository.findById(reportId).orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo."));
        if (report.getStatus() != CourseDiscussionReportStatus.PENDING) {
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
