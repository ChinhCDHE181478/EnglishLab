package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.response.assessment.PlacementRecommendationResponse;

/** Turns a scored placement attempt into course / program / learning-path suggestions. */
public interface PlacementRecommendationService {
    PlacementRecommendationResponse getRecommendations(Long attemptId, String learnerEmail);
}
