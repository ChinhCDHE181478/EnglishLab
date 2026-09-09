package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.request.course.LearningPathCoursesRequest;
import fu.sep490.g23.backend.dto.request.course.LearningPathRequest;
import fu.sep490.g23.backend.dto.response.course.LearnerLearningPathCourseResponse;
import fu.sep490.g23.backend.dto.response.course.LearnerLearningPathResponse;
import fu.sep490.g23.backend.dto.response.course.LearningPathResponse;
import fu.sep490.g23.backend.dto.response.course.LearningPathOfferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/** Coordinates learning-path management, recommendations, and public offers. */
public interface LearningPathManagementService {
    /** Returns learning paths for the Content Manager screen. */
    Page<LearningPathResponse> getManagedPaths(Pageable pageable);

    /** Creates a learning path without attached courses. */
    LearningPathResponse createPath(LearningPathRequest request);

    /** Updates learning-path metadata. */
    LearningPathResponse updatePath(Long pathId, LearningPathRequest request);

    /** Appends new courses and skips courses already attached. */
    LearningPathResponse addCourses(Long pathId, LearningPathCoursesRequest request);

    /** Reorders all attached courses using IDs in the requested order. */
    LearningPathResponse reorderCourses(Long pathId, LearningPathCoursesRequest request);

    /** Deletes a path and its course relationships. */
    void deletePath(Long pathId);

    /** Builds the recommended path for the current learner. */
    LearnerLearningPathResponse getMyLearningPath(String studentEmail);

    /** Returns public path offers with learner ownership state. */
    java.util.List<LearningPathOfferResponse> getPublicOffers(String studentEmail);

    /** Returns public path offers as a page. */
    org.springframework.data.domain.Page<LearningPathOfferResponse> getPublicOffers(String studentEmail, org.springframework.data.domain.Pageable pageable);

    /** Returns one public path offer by code. */
    LearningPathOfferResponse getPublicOffer(String code, String studentEmail);
}
