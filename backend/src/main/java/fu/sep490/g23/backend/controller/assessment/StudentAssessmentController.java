package fu.sep490.g23.backend.controller.assessment;

import fu.sep490.g23.backend.dto.request.assessment.AssessmentSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.AssessmentAudioUploadResponse;
import fu.sep490.g23.backend.dto.response.assessment.AiAssessmentSubmissionResponse;
import fu.sep490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import fu.sep490.g23.backend.service.assessment.AssessmentAudioStorageService;
import fu.sep490.g23.backend.service.assessment.AiAssessmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentAssessmentController {

    private final AiAssessmentService aiAssessmentService;
    private final AssessmentAudioStorageService assessmentAudioStorageService;

    // Get course assessments for a specific course and the authenticated student.
    @GetMapping({"/online-courses/{courseId}/assessments", "/courses/{courseId}/assessments"})
    public ResponseEntity<List<CourseAssessmentResponse>> getCourseAssessments(@PathVariable Long courseId, Authentication authentication) {
        return ResponseEntity.ok(aiAssessmentService.getCourseAssessments(courseId, authentication.getName()));
    }

    @PostMapping("/assessments/{assessmentId}/submit")
    public ResponseEntity<AiAssessmentSubmissionResponse> submitAssessment(
            @PathVariable Long assessmentId,
            @Valid @RequestBody AssessmentSubmissionRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(aiAssessmentService.submitAssessment(assessmentId, request, authentication.getName()));
    }

    @PostMapping(value = "/assessments/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssessmentAudioUploadResponse> uploadAssessmentAudio(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        String publicUrlBase = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/student/assessments/audio")
                .toUriString();
        return ResponseEntity.ok(assessmentAudioStorageService.store(file, publicUrlBase));
    }

    @GetMapping("/assessments/audio/{fileName}")
    public ResponseEntity<Resource> getAssessmentAudio(
            @PathVariable String fileName,
            Authentication authentication
    ) {
        Resource resource = assessmentAudioStorageService.loadAsResource(fileName);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, assessmentAudioStorageService.detectContentType(fileName))
                .body(resource);
    }
}
