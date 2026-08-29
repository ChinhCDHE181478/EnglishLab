package fu.sep490.g23.backend.controller.assessment;

import fu.sep490.g23.backend.dto.request.assessment.PlacementTestSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;
import fu.sep490.g23.backend.service.assessment.PlacementTestService;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationService;
import fu.sep490.g23.backend.dto.response.assessment.PlacementRecommendationResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Student APIs for the placement-test journey: load paper -> submit/score -> read recommendations.
 */
@RestController
@RequestMapping("/api/student/placement-tests")
@RequiredArgsConstructor
public class PlacementTestController {

}
