package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.CourseReviewRequest;
import fu.sap490.g23.backend.dto.response.course.CourseRatingResponse;

public interface CourseReviewService {
    CourseRatingResponse getMyRating(Long courseId, String studentEmail);
    CourseRatingResponse saveRating(Long courseId, CourseReviewRequest request, String studentEmail);
}
