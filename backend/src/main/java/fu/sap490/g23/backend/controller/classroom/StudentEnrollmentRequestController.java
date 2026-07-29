package fu.sap490.g23.backend.controller.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CreateCourseEnrollmentRequest;
import fu.sap490.g23.backend.dto.response.classroom.CourseEnrollmentRequestResponse;
import fu.sap490.g23.backend.service.classroom.EnrollmentRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/student/course-enrollment-requests")
@RequiredArgsConstructor
public class StudentEnrollmentRequestController {
    private final EnrollmentRequestService enrollmentRequestService;

    @PostMapping
    public ResponseEntity<CourseEnrollmentRequestResponse> submit(
            @Valid @RequestBody CreateCourseEnrollmentRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(enrollmentRequestService.submit(request, authentication.getName()));
    }

    @GetMapping("/my")
    public ResponseEntity<List<CourseEnrollmentRequestResponse>> listMine(Authentication authentication) {
        return ResponseEntity.ok(enrollmentRequestService.listMine(authentication.getName()));
    }

}
