package fu.sep490.g23.backend.controller.course;

import fu.sep490.g23.backend.dto.response.course.LearningPathOfferResponse;
import fu.sep490.g23.backend.service.course.LearningPathManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;

@RestController
@RequestMapping("/api/learning-paths")
@RequiredArgsConstructor
public class PublicLearningPathController {
    private final LearningPathManagementService learningPathManagementService;

    @GetMapping
    public ResponseEntity<List<LearningPathOfferResponse>> getOffers(Authentication authentication) {
        return ResponseEntity.ok(learningPathManagementService.getPublicOffers(emailOf(authentication)));
    }

    @GetMapping("/page")
    public ResponseEntity<Page<LearningPathOfferResponse>> getOffersPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication
    ) {
        return ResponseEntity.ok(learningPathManagementService.getPublicOffers(
                emailOf(authentication),
                PageRequest.of(page, size, Sort.by("name").ascending())
        ));
    }

    @GetMapping("/{code}")
    public ResponseEntity<LearningPathOfferResponse> getOffer(
            @PathVariable String code,
            Authentication authentication
    ) {
        return ResponseEntity.ok(learningPathManagementService.getPublicOffer(code, emailOf(authentication)));
    }

    private String emailOf(Authentication authentication) {
        return authentication == null || !authentication.isAuthenticated() ? null : authentication.getName();
    }
}
