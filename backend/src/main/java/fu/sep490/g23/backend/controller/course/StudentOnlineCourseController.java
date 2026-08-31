package fu.sep490.g23.backend.controller.course;

import fu.sep490.g23.backend.dto.response.course.CourseCertificateResponse;
import fu.sep490.g23.backend.dto.response.course.CourseCompletionResponse;
import fu.sep490.g23.backend.dto.request.course.CourseReviewRequest;
import fu.sep490.g23.backend.dto.response.course.CourseRatingResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseEnrollmentResponse;
import fu.sep490.g23.backend.dto.response.course.VocabularyTermResponse;
import fu.sep490.g23.backend.entity.course.enums.VocabularyProgressStatus;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import fu.sep490.g23.backend.service.course.CourseReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/online-courses")
@RequiredArgsConstructor
public class StudentOnlineCourseController {

    private final OnlineCourseService onlineCourseService;
    private final CourseReviewService courseReviewService;

    @PostMapping("/{courseId}/register")
    public ResponseEntity<OnlineCourseResponse> registerCourse(@PathVariable Long courseId, Authentication authentication) {
        return ResponseEntity.ok(onlineCourseService.registerCourse(courseId, authentication.getName()));
    }

    /**
     * Retrieves the detailed content of an online course that the current user is enrolled in.
     *
     * @param courseId       the ID of the course
     * @param authentication current user's authentication info
     * @return Detailed information about the enrolled course
     */
    @GetMapping("/{courseId}/content")
    public ResponseEntity<OnlineCourseResponse> getEnrolledCourse(@PathVariable Long courseId, Authentication authentication) {
        return ResponseEntity.ok(onlineCourseService.getEnrolledCourse(courseId, authentication.getName()));
    }

    /**
     * Retrieves the current user's enrolled online courses.
     *
     * @param authentication current user's authentication info
     * @return List of enrolled courses with progress
     */
    @GetMapping({"/my-enrollments", "/my-courses"})
    public ResponseEntity<List<OnlineCourseEnrollmentResponse>> getMyEnrollments(Authentication authentication) {
        return ResponseEntity.ok(onlineCourseService.getMyEnrollments(authentication.getName()));
    }

    @GetMapping("/recommendations")
    public ResponseEntity<List<OnlineCourseResponse>> getRecommendations(Authentication authentication) {
        return ResponseEntity.ok(onlineCourseService.getRecommendedCourses(authentication.getName()));
    }

    @GetMapping("/{courseId}/completion")
    public ResponseEntity<CourseCompletionResponse> getCourseCompletion(
            @PathVariable Long courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlineCourseService.getCourseCompletion(courseId, authentication.getName()));
    }

    @GetMapping("/{courseId}/certificate")
    public ResponseEntity<CourseCertificateResponse> getCourseCertificate(
            @PathVariable Long courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlineCourseService.getCourseCertificate(courseId, authentication.getName()));
    }

    @GetMapping("/{courseId}/rating")
    public ResponseEntity<CourseRatingResponse> getMyRating(@PathVariable Long courseId, Authentication authentication) {
        return ResponseEntity.ok(courseReviewService.getMyRating(courseId, authentication.getName()));
    }

    @PostMapping("/{courseId}/rating")
    public ResponseEntity<CourseRatingResponse> saveRating(
            @PathVariable Long courseId,
            @Valid @RequestBody CourseReviewRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(courseReviewService.saveRating(courseId, request, authentication.getName()));
    }

    /**
     * Updates the progress of a specific lesson in a course for the authenticated user.
     * This endpoint marks the lesson as completed or in progress.
     *
     * @param courseId       the ID of the course
     * @param lessonId       the ID of the lesson to update
     * @param completed      true to mark as completed, false to mark as in progress (defaults to true)
     * @param authentication current user's authentication info
     * @return Updated course enrollment information reflecting the new progress
     */
    @PatchMapping("/{courseId}/lessons/{lessonId}/progress")
    public ResponseEntity<OnlineCourseEnrollmentResponse> updateLessonProgress(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @RequestParam(defaultValue = "true") boolean completed,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlineCourseService.updateLessonProgress(courseId, lessonId, completed, authentication.getName()));
    }

    @GetMapping("/{courseId}/vocabulary")
    public ResponseEntity<List<VocabularyTermResponse>> getVocabularyTerms(
            @PathVariable Long courseId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlineCourseService.getVocabularyTerms(courseId, authentication.getName()));
    }

    @PatchMapping("/{courseId}/vocabulary/{termKey}/progress")
    public ResponseEntity<VocabularyTermResponse> updateVocabularyProgress(
            @PathVariable Long courseId,
            @PathVariable String termKey,
            @RequestParam(required = false) VocabularyProgressStatus status,
            @RequestParam(required = false) Boolean starred,
            @RequestParam(required = false) Boolean reviewed,
            @RequestParam(required = false) Boolean correct,
            Authentication authentication
    ) {
        return ResponseEntity.ok(onlineCourseService.updateVocabularyProgress(courseId, termKey, status, starred, reviewed, correct, authentication.getName()));
    }
}
