package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.response.course.LearnerLearningPathResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationContext;

public interface LearningPathRecommendationService {
    /** Keep the learner on a path they already started when true. */
    LearnerLearningPathResponse.PathOverview recommend(
            User learner,
            PlacementRecommendationContext context,
            boolean preserveActivePath
    );
}
