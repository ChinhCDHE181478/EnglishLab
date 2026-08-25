package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.exception.EnrollmentAccessException;
import fu.sep490.g23.backend.exception.EnrollmentErrorCode;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CourseEnrollmentAccessPolicyImpl implements CourseEnrollmentAccessPolicy {

    static final String CANCELLED_MESSAGE = "Bạn đã hủy đăng ký khóa học này. Vui lòng đăng ký lại để tiếp tục.";
    static final String INVALID_STATUS_MESSAGE = "Không thể xác định trạng thái đăng ký. Vui lòng đăng ký lại khóa học.";

    private final OnlineCourseEnrollmentRepository enrollmentRepository;

    @Override
    public boolean hasLearningAccess(OnlineCourseEnrollment enrollment) {
        return hasActiveEnrollmentStatus(enrollment);
    }

    @Override
    public boolean hasAssessmentAccess(OnlineCourseEnrollment enrollment) {
        return hasActiveEnrollmentStatus(enrollment);
    }

    @Override
    public OnlineCourseEnrollment requireLearningAccess(User student, OnlineCourse course) {
        OnlineCourseEnrollment enrollment = findEnrollment(
                student,
                course,
                "Bạn cần đăng ký khóa học trước khi xem nội dung."
        );
        ensureLearningAccess(enrollment);
        return enrollment;
    }

    @Override
    public OnlineCourseEnrollment requireAssessmentAccess(User student, OnlineCourse course) {
        OnlineCourseEnrollment enrollment = findEnrollment(
                student,
                course,
                "Bạn cần đăng ký khóa học trước khi làm bài đánh giá."
        );
        ensureAssessmentAccess(enrollment);
        return enrollment;
    }

    @Override
    public OnlineCourseEnrollment reactivateCancelledEnrollment(OnlineCourseEnrollment enrollment) {
        if (enrollment == null || enrollment.getStatus() != EnrollmentStatus.CANCELLED) {
            return enrollment;
        }
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment.setRegisteredAt(LocalDateTime.now());
        return enrollmentRepository.save(enrollment);
    }

    private boolean hasActiveEnrollmentStatus(OnlineCourseEnrollment enrollment) {
        if (enrollment == null || enrollment.getStatus() == null) {
            return false;
        }
        EnrollmentStatus status = enrollment.getStatus();
        return status == EnrollmentStatus.ACTIVE || status == EnrollmentStatus.COMPLETED;
    }

    private OnlineCourseEnrollment findEnrollment(User student, OnlineCourse course, String missingMessage) {
        return enrollmentRepository.findByStudentAndOnlineCourse(student, course)
                .or(() -> enrollmentRepository.findByStudentAndLearningPackage(student, course.getLearningPackage()))
                .orElseThrow(() -> new EnrollmentAccessException(EnrollmentErrorCode.NOT_ENROLLED, missingMessage));
    }

    private void ensureLearningAccess(OnlineCourseEnrollment enrollment) {
        if (!hasLearningAccess(enrollment)) {
            throw accessDeniedException(enrollment);
        }
    }

    private void ensureAssessmentAccess(OnlineCourseEnrollment enrollment) {
        if (!hasAssessmentAccess(enrollment)) {
            throw accessDeniedException(enrollment);
        }
    }

    private EnrollmentAccessException accessDeniedException(OnlineCourseEnrollment enrollment) {
        if (enrollment.getStatus() == EnrollmentStatus.CANCELLED) {
            return new EnrollmentAccessException(EnrollmentErrorCode.ENROLLMENT_CANCELLED, CANCELLED_MESSAGE);
        }
        return new EnrollmentAccessException(EnrollmentErrorCode.ENROLLMENT_INVALID_STATUS, INVALID_STATUS_MESSAGE);
    }
}
