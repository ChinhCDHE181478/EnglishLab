package fu.sap490.g23.backend.service.course.impl;

import fu.sap490.g23.backend.service.course.*;


import fu.sap490.g23.backend.dto.request.course.CourseReviewRequest;
import fu.sap490.g23.backend.dto.response.course.CourseCompletionResponse;
import fu.sap490.g23.backend.dto.response.course.CourseRatingResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.CourseReview;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.CourseReviewRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @RequiredArgsConstructor
public class CourseReviewServiceImpl implements CourseReviewService {
    private final UserRepository userRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final CourseReviewRepository reviewRepository;
    private final CourseProgressService courseProgressService;
    private final CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;

    @Override @Transactional
    public CourseRatingResponse getMyRating(Long courseId, String studentEmail) {
        User student = findStudent(studentEmail); OnlineCourse course = findCourse(courseId);
        return toResponse(course, reviewRepository.findByStudentAndCourse(student, course).orElse(null));
    }

    @Override @Transactional
    public CourseRatingResponse saveRating(Long courseId, CourseReviewRequest request, String studentEmail) {
        User student = findStudent(studentEmail); OnlineCourse course = findCourse(courseId);
        PackageEnrollment enrollment = courseEnrollmentAccessPolicy.requireLearningAccess(student, course);
        CourseCompletionResponse completion = courseProgressService.buildCompletionResponse(enrollment, course, student);
        if (!completion.isEligibleForCertificate()) throw new RuntimeException("Bạn chỉ có thể đánh giá sau khi hoàn thành khóa học.");
        CourseReview review = reviewRepository.findByStudentAndCourse(student, course)
                .orElseGet(() -> CourseReview.builder().student(student).course(course).build());
        review.setRating(request.getRating()); review.setComment(normalizeComment(request.getComment()));
        return toResponse(course, reviewRepository.save(review));
    }

    private User findStudent(String email) { return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng.")); }
    private OnlineCourse findCourse(Long courseId) { return onlineCourseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học.")); }
    private CourseRatingResponse toResponse(OnlineCourse course, CourseReview myReview) {
        Double average = reviewRepository.findAverageRatingByCourse(course);
        return CourseRatingResponse.builder().courseId(course.getId()).averageRating(average == null ? 0D : Math.round(average * 10D) / 10D)
                .reviewCount(reviewRepository.countByCourse(course)).myRating(myReview == null ? null : myReview.getRating())
                .myComment(myReview == null ? null : myReview.getComment()).updatedAt(myReview == null ? null : myReview.getUpdatedAt()).build();
    }
    private String normalizeComment(String comment) { return comment == null || comment.isBlank() ? null : comment.trim(); }
}
