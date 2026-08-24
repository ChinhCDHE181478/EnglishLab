package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.request.course.LearningPathCoursesRequest;
import fu.sep490.g23.backend.dto.request.course.LearningPathRequest;
import fu.sep490.g23.backend.dto.response.course.LearnerLearningPathCourseResponse;
import fu.sep490.g23.backend.dto.response.course.LearnerLearningPathResponse;
import fu.sep490.g23.backend.dto.response.course.LearningPathResponse;
import fu.sep490.g23.backend.dto.response.course.LearningPathOfferResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LearningPathManagementService {
    Page<LearningPathResponse> getManagedPaths(Pageable pageable);
    LearningPathResponse createPath(LearningPathRequest request);
    LearningPathResponse updatePath(Long pathId, LearningPathRequest request);
    LearningPathResponse addCourses(Long pathId, LearningPathCoursesRequest request);
    LearningPathResponse reorderCourses(Long pathId, LearningPathCoursesRequest request);
    void deletePath(Long pathId);
    LearnerLearningPathResponse getMyLearningPath(String studentEmail);
    java.util.List<LearningPathOfferResponse> getPublicOffers(String studentEmail);
    org.springframework.data.domain.Page<LearningPathOfferResponse> getPublicOffers(String studentEmail, org.springframework.data.domain.Pageable pageable);
    LearningPathOfferResponse getPublicOffer(String code, String studentEmail);
}
