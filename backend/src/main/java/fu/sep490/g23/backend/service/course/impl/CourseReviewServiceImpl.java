package fu.sep490.g23.backend.service.course.impl;

import fu.sep490.g23.backend.dto.request.course.CourseReviewRequest;
import fu.sep490.g23.backend.dto.response.course.CourseCompletionResponse;
import fu.sep490.g23.backend.dto.response.course.CourseRatingResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sep490.g23.backend.service.course.CourseProgressService;
import fu.sep490.g23.backend.service.course.CourseReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CourseReviewServiceImpl implements CourseReviewService {
    private final UserRepository userRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseEnrollmentRepository enrollmentRepository;
    private final CourseProgressService courseProgressService;
    private final CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;

    @Override
    @Transactional(readOnly = true)
    public CourseRatingResponse getMyRating(Long courseId, String studentEmail) {
        User student = findStudent(studentEmail);
        OnlineCourse course = findCourse(courseId);
        OnlineCourseEnrollment enrollment = enrollmentRepository.findByStudentAndOnlineCourse(student, course).orElse(null);
        return toResponse(course, enrollment);
    }

    @Override
    @Transactional
    public CourseRatingResponse saveRating(Long courseId, CourseReviewRequest request, String studentEmail) {
        User student = findStudent(studentEmail);
        OnlineCourse course = findCourse(courseId);
        OnlineCourseEnrollment enrollment = courseEnrollmentAccessPolicy.requireLearningAccess(student, course);
        CourseCompletionResponse completion = courseProgressService.buildCompletionResponse(enrollment, course, student);
        if (!completion.isEligibleForCertificate()) {
            throw new RuntimeException("Bạn chỉ có thể đánh giá sau khi hoàn thành khóa học.");
        }
        enrollment.setReviewRating(request.getRating());
        enrollment.setReviewComment(normalizeComment(request.getComment()));
        enrollment.setReviewedAt(LocalDateTime.now());
        return toResponse(course, enrollmentRepository.save(enrollment));
    }

    private User findStudent(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    private OnlineCourse findCourse(Long courseId) {
        return onlineCourseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
    }

    private CourseRatingResponse toResponse(OnlineCourse course, OnlineCourseEnrollment enrollment) {
        Double average = enrollmentRepository.findAverageReviewRatingByOnlineCourse(course);
        long reviewCount = enrollmentRepository.countByOnlineCourseAndReviewRatingIsNotNull(course);
        return CourseRatingResponse.builder()
                .courseId(course.getId())
                .averageRating(average == null ? 0D : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP).doubleValue())
                .reviewCount(reviewCount)
                .myRating(enrollment == null ? null : enrollment.getReviewRating())
                .myComment(enrollment == null ? null : enrollment.getReviewComment())
                .updatedAt(enrollment == null ? null : enrollment.getReviewedAt())
                .build();
    }

    private String normalizeComment(String comment) {
        return comment == null || comment.isBlank() ? null : comment.trim();
    }
}
