package fu.sep490.g23.backend.controller.assessment;

import fu.sep490.g23.backend.dto.request.assessment.MockTestSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.MockTestAttemptResponse;
import fu.sep490.g23.backend.dto.response.curriculum.AssessmentBankItemResponse;
import fu.sep490.g23.backend.service.assessment.MockTestService;
import fu.sep490.g23.backend.service.curriculum.InstructorLedCourseManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/mock-tests")
@RequiredArgsConstructor
public class StudentMockTestController {

    private final InstructorLedCourseManagementService instructorLedCourseManagementService;
    private final MockTestService mockTestService;

    @GetMapping
    public ResponseEntity<List<AssessmentBankItemResponse>> listMockTests(Authentication authentication) {
        return ResponseEntity.ok(instructorLedCourseManagementService.listPublishedMockTests());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssessmentBankItemResponse> getMockTest(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return ResponseEntity.ok(instructorLedCourseManagementService.getPublishedMockTest(id));
    }

    @PostMapping("/{id}/submit")
    public ResponseEntity<MockTestAttemptResponse> submitMockTest(
            @PathVariable Long id,
            @Valid @RequestBody MockTestSubmissionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(mockTestService.submitMockTest(id, request, authentication.getName()));
    }
}
