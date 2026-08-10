package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.LearningPathCoursesRequest;
import fu.sap490.g23.backend.dto.request.course.LearningPathRequest;
import fu.sap490.g23.backend.dto.response.course.LearnerLearningPathCourseResponse;
import fu.sap490.g23.backend.dto.response.course.LearnerLearningPathResponse;
import fu.sap490.g23.backend.dto.response.course.LearningPathResponse;
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
}
