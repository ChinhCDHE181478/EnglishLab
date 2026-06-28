package fu.sap490.g23.backend.service.course.impl;

import fu.sap490.g23.backend.service.course.*;

import fu.sap490.g23.backend.dto.response.course.CourseCompletionResponse;
import fu.sap490.g23.backend.dto.response.course.CourseCompletionStatus;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.enums.SubmissionStatus;
import fu.sap490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sap490.g23.backend.entity.course.LessonProgress;
import fu.sap490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sap490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sap490.g23.backend.repository.course.LessonProgressRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
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
    private final PackageEnrollmentRepository enrollmentRepository;

    public PackageEnrollment refreshEnrollmentProgress(PackageEnrollment enrollment, OnlineCourse course, User student) {
        CompletionSnapshot snapshot = buildSnapshot(enrollment, course, student);

        long totalItems = (long) snapshot.totalLessons() + snapshot.totalAssessments();
        long completedItems = (long) snapshot.completedLessons() + snapshot.completedAssessments();
        enrollment.setProgressPercent(totalItems == 0 ? 0 : (int) Math.round((completedItems * 100.0) / totalItems));
        if (enrollment.getStatus() != EnrollmentStatus.CANCELLED) {
            enrollment.setStatus(snapshot.eligibleForCertificate() ? EnrollmentStatus.COMPLETED : EnrollmentStatus.ACTIVE);
        }
        return enrollmentRepository.save(enrollment);
    }

    public void refreshCourseEnrollments(OnlineCourse course) {
        List<PackageEnrollment> enrollments = enrollmentRepository.findByLearningPackage(course.getLearningPackage());
        for (PackageEnrollment enrollment : enrollments) {
            if (enrollment.getStatus() != EnrollmentStatus.CANCELLED) {
                refreshEnrollmentProgress(enrollment, course, enrollment.getStudent());
            }
        }
    }

    public CourseCompletionResponse buildCompletionResponse(PackageEnrollment enrollment, OnlineCourse course, User student) {
        CompletionSnapshot snapshot = buildSnapshot(enrollment, course, student);

        CourseCompletionStatus status = resolveStatus(snapshot, enrollment);
        String statusReason = resolveStatusReason(snapshot, status);
        boolean eligibleForCertificate = snapshot.eligibleForCertificate();

        return CourseCompletionResponse.builder()
                .courseId(course.getId())
                .enrollmentId(enrollment.getId())
                .courseTitle(course.getLearningPackage().getTitle())
                .courseSlug(course.getLearningPackage().getSlug())
                .progressPercent(enrollment.getProgressPercent())
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

    private CompletionSnapshot buildSnapshot(PackageEnrollment enrollment, OnlineCourse course, User student) {
        int totalLessons = course.getModules().stream()
                .mapToInt(module -> module.getLessons().size())
                .sum();
        int totalAssessments = Math.toIntExact(courseAssessmentRepository.countByOnlineCourseAndActiveTrue(course));
        int completedLessons = Math.toIntExact(lessonProgressRepository.countByEnrollmentAndStatus(enrollment, LessonProgressStatus.COMPLETED));
        int completedAssessments = Math.toIntExact(assessmentSubmissionRepository.countCompletedAssessments(student, course, COMPLETED_ASSESSMENT_STATUSES));
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

    private CourseCompletionStatus resolveStatus(CompletionSnapshot snapshot, PackageEnrollment enrollment) {
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
