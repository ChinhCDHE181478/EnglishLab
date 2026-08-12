package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.dto.response.assessment.PlacementRecommendationResponse;

public interface PlacementRecommendationService {
    PlacementRecommendationResponse getRecommendations(Long attemptId, String learnerEmail);
}
