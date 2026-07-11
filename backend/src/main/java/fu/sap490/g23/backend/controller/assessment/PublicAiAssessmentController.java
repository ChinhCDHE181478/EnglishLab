package fu.sap490.g23.backend.controller.assessment;

import fu.sap490.g23.backend.dto.request.assessment.WritingFeedbackRequest;
import fu.sap490.g23.backend.dto.response.assessment.WritingFeedbackResponse;
import fu.sap490.g23.backend.service.assessment.AiAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class PublicAiAssessmentController {
    private final AiAssessmentService aiAssessmentService;

    @PostMapping("/writing-feedback/demo")
    public ResponseEntity<WritingFeedbackResponse> writingFeedbackDemo(@Valid @RequestBody WritingFeedbackRequest request) {
        return ResponseEntity.ok(aiAssessmentService.evaluateWritingFeedback(request));
    }
}
