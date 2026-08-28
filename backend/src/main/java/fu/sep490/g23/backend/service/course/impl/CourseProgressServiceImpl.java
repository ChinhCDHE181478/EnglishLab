package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.dto.response.course.CourseCompletionResponse;
import fu.sep490.g23.backend.dto.response.course.CourseCompletionStatus;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.assessment.enums.SubmissionStatus;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.LessonProgress;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.service.course.CourseProgressService;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseProgressServiceImpl implements CourseProgressService {
    private static final Set<SubmissionStatus> COMPLETED_ASSESSMENT_STATUSES = Set.of(
            SubmissionStatus.AI_EVALUATED,
            SubmissionStatus.PASSED
    );

    private final LessonProgressRepository lessonProgressRepository;
    private final CourseAssessmentRepository courseAssessmentRepository;
    private final AssessmentSubmissionRepository assessmentSubmissionRepository;
    private final OnlineCourseEnrollmentRepository enrollmentRepository;
    private final OnlineCourseVersionService onlineCourseVersionService;

    /**
     * Refreshes the progress percent and status of an enrollment.
     * 
     * @param enrollment The current enrollment
     * @param course     The associated course
     * @param student    The student
     * @return The updated enrollment (or unchanged if no updates needed)
     */
    public OnlineCourseEnrollment refreshEnrollmentProgress(OnlineCourseEnrollment enrollment, OnlineCourse course, User student) {
        // Build a snapshot of required vs completed lessons/assessments
        CompletionSnapshot snapshot = buildSnapshot(enrollment, course, student);

        // Calculate progress percentage based on snapshot
        int progressPercent = calculateProgressPercent(snapshot);
        
        // Determine new status: keep CANCELLED if already cancelled. 
        // If eligible for certificate, mark as COMPLETED, otherwise ACTIVE.
        EnrollmentStatus nextStatus = enrollment.getStatus() == EnrollmentStatus.CANCELLED
                ? EnrollmentStatus.CANCELLED
                : snapshot.eligibleForCertificate() ? EnrollmentStatus.COMPLETED : EnrollmentStatus.ACTIVE;
                
        // If nothing changed, return early to avoid unnecessary DB updates
        if (java.util.Objects.equals(enrollment.getProgressPercent(), progressPercent)
                && enrollment.getStatus() == nextStatus) {
            return enrollment;
        }
        
        // Update progress and save to DB
        enrollment.setProgressPercent(progressPercent);
        enrollment.setStatus(nextStatus);
        return enrollmentRepository.save(enrollment);
    }

    public CourseCompletionResponse buildCompletionResponse(OnlineCourseEnrollment enrollment, OnlineCourse course, User student) {
        CompletionSnapshot snapshot = buildSnapshot(enrollment, course, student);

        CourseCompletionStatus status = resolveStatus(snapshot, enrollment);
        String statusReason = resolveStatusReason(snapshot, status);
        boolean eligibleForCertificate = snapshot.eligibleForCertificate();
        int progressPercent = calculateProgressPercent(snapshot);

        return CourseCompletionResponse.builder()
                .courseId(course.getId())
                .enrollmentId(enrollment.getId())
                .courseTitle(course.getTitle())
                .courseSlug(course.getSlug())
                .progressPercent(progressPercent)
                .totalLessons(snapshot.totalLessons())
                .completedLessons(snapshot.completedLessons())
                .totalAssessments(snapshot.totalAssessments())
                .completedAssessments(snapshot.completedAssessments())
                .completedRequiredLessons(snapshot.completedRequiredLessons())
                .completedRequiredAssessments(snapshot.completedRequiredAssessments())
                .eligibleForCertificate(eligibleForCertificate)
                .status(status)
                .statusReason(statusReason)
                .completionDate(eligibleForCertificate ? enrollment.getUpdatedAt() : null)
                .latestLessonId(snapshot.latestLessonId())
                .latestLessonTitle(snapshot.latestLessonTitle())
                .latestLessonAccessedAt(snapshot.latestLessonAccessedAt())
                .build();
    }

    private int calculateProgressPercent(CompletionSnapshot snapshot) {
        long totalItems = (long) snapshot.totalLessons() + snapshot.totalAssessments();
        long completedItems = (long) snapshot.completedLessons() + snapshot.completedAssessments();
        return totalItems == 0 ? 0 : (int) Math.round((completedItems * 100.0) / totalItems);
    }

    private CompletionSnapshot buildSnapshot(OnlineCourseEnrollment enrollment, OnlineCourse course, User student) {
        int liveLessonCount = course.getModules().stream()
                .mapToInt(module -> module.getLessons().size())
                .sum();
        int liveAssessmentCount = Math.toIntExact(courseAssessmentRepository.countByOnlineCourseAndActiveTrue(course));
        int totalLessons = enrollment.getCourseVersion() == null
                ? liveLessonCount
                : enrollment.getCourseVersion().getTotalRequiredLessons();
        int totalAssessments = enrollment.getCourseVersion() == null
                ? liveAssessmentCount
                : enrollment.getCourseVersion().getTotalRequiredAssessments();
        int completedLessons = Math.min(
                totalLessons,
                Math.toIntExact(lessonProgressRepository.countByEnrollmentAndStatus(enrollment, LessonProgressStatus.COMPLETED))
        );
        List<CourseAssessment> baselineAssessments = courseAssessmentRepository.findAllById(
                onlineCourseVersionService.getProgressBaselineAssessmentIds(enrollment)
        );
        int completedAssessments = Math.min(totalAssessments, (int) baselineAssessments.stream()
                .filter(assessment -> hasCompletedSubmission(assessment, student))
                .count());
        boolean completedRequiredLessons = totalLessons > 0 && completedLessons >= totalLessons;
        boolean completedRequiredAssessments = totalAssessments == 0 || completedAssessments >= totalAssessments;
        boolean hasEnoughDataForCertificate = totalLessons > 0;
        boolean eligibleForCertificate = hasEnoughDataForCertificate && completedRequiredLessons && completedRequiredAssessments;

        LessonProgress latestProgress = lessonProgressRepository.findByEnrollment(enrollment).stream()
                .filter(progress -> progress.getLastAccessedAt() != null || progress.getUpdatedAt() != null || progress.getCreatedAt() != null)
                .max((left, right) -> resolveLatestAccessTime(left).compareTo(resolveLatestAccessTime(right)))
                .orElse(null);

        return new CompletionSnapshot(
                totalLessons,
                completedLessons,
                totalAssessments,
                completedAssessments,
                completedRequiredLessons,
                completedRequiredAssessments,
                eligibleForCertificate,
                hasEnoughDataForCertificate,
                latestProgress == null ? null : latestProgress.getLesson().getId(),
                latestProgress == null ? null : latestProgress.getLesson().getTitle(),
                latestProgress == null ? null : resolveLatestAccessTime(latestProgress)
        );
    }

    private boolean hasCompletedSubmission(CourseAssessment assessment, User student) {
        if (assessment.getProgressKey() == null || assessment.getProgressKey().isBlank()) {
            return assessmentSubmissionRepository.existsByAssessmentAndStudentAndStatusIn(
                    assessment,
                    student,
                    COMPLETED_ASSESSMENT_STATUSES
            );
        }
        boolean completedInLineage = assessmentSubmissionRepository.existsByAssessmentProgressKeyAndStudentAndStatusIn(
                assessment.getProgressKey(),
                student,
                COMPLETED_ASSESSMENT_STATUSES
        );
        return completedInLineage || assessmentSubmissionRepository.existsByAssessmentAndStudentAndStatusIn(
                assessment,
                student,
                COMPLETED_ASSESSMENT_STATUSES
        );
    }

    private CourseCompletionStatus resolveStatus(CompletionSnapshot snapshot, OnlineCourseEnrollment enrollment) {
        if (snapshot.completedLessons() == 0 && snapshot.completedAssessments() == 0) {
            return CourseCompletionStatus.CHUA_BAT_DAU;
        }
        if (snapshot.eligibleForCertificate()) {
            return enrollment.getStatus() == EnrollmentStatus.COMPLETED
                    ? CourseCompletionStatus.DA_HOAN_THANH
                    : CourseCompletionStatus.DU_DIEU_KIEN_NHAN_CHUNG_NHAN;
        }
        if (snapshot.completedRequiredLessons() && !snapshot.completedRequiredAssessments()) {
            return CourseCompletionStatus.CAN_HOAN_THANH_BAI_DANH_GIA;
        }
        return CourseCompletionStatus.DANG_HOC;
    }

    private String resolveStatusReason(CompletionSnapshot snapshot, CourseCompletionStatus status) {
        if (!snapshot.hasEnoughDataForCertificate()) {
            return "Khóa học này chưa có đủ dữ liệu để xác nhận hoàn thành.";
        }
        return switch (status) {
            case CHUA_BAT_DAU -> "Bạn chưa bắt đầu khóa học này.";
            case DANG_HOC -> "Bạn vẫn đang hoàn thành các bài học bắt buộc của khóa học.";
            case CAN_HOAN_THANH_BAI_DANH_GIA -> "Bạn cần hoàn thành bài đánh giá bắt buộc để nhận chứng nhận hoàn thành.";
            case DU_DIEU_KIEN_NHAN_CHUNG_NHAN -> "Bạn đã đủ điều kiện nhận chứng nhận hoàn thành.";
            case DA_HOAN_THANH -> "Bạn đã hoàn thành khóa học và có thể sử dụng chứng nhận hoàn thành.";
        };
    }

    private java.time.LocalDateTime resolveLatestAccessTime(LessonProgress progress) {
        if (progress.getLastAccessedAt() != null) {
            return progress.getLastAccessedAt();
        }
        if (progress.getUpdatedAt() != null) {
            return progress.getUpdatedAt();
        }
        return progress.getCreatedAt();
    }

    private record CompletionSnapshot(
            int totalLessons,
            int completedLessons,
            int totalAssessments,
            int completedAssessments,
            boolean completedRequiredLessons,
            boolean completedRequiredAssessments,
            boolean eligibleForCertificate,
            boolean hasEnoughDataForCertificate,
            Long latestLessonId,
            String latestLessonTitle,
            java.time.LocalDateTime latestLessonAccessedAt
    ) {
    }
}
