package fu.sap490.g23.backend.controller.course;

import fu.sap490.g23.backend.dto.response.course.CourseCertificateResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.entity.assessment.AssessmentSkill;
import fu.sap490.g23.backend.entity.course.CourseCategoryCode;
import fu.sap490.g23.backend.service.course.OnlineCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/online-courses")
@RequiredArgsConstructor
public class PublicOnlineCourseController {

    private final OnlineCourseService onlineCourseService;

    @GetMapping
    public ResponseEntity<Page<OnlineCourseResponse>> getCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) CourseCategoryCode category,
            @RequestParam(required = false) Double currentBand,
            @RequestParam(required = false) Double targetBand,
            @RequestParam(required = false) AssessmentSkill skill,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(onlineCourseService.getPublicCourses(keyword, category, currentBand, targetBand, skill, pageable));
    }

    @GetMapping("/{slugOrId}")
    public ResponseEntity<OnlineCourseResponse> getCourse(@PathVariable String slugOrId) {
        return ResponseEntity.ok(onlineCourseService.getPublicCourse(slugOrId));
    }

    @GetMapping("/certificates/{verificationCode}")
    public ResponseEntity<CourseCertificateResponse> verifyCertificate(@PathVariable String verificationCode) {
        return ResponseEntity.ok(onlineCourseService.verifyCourseCertificate(verificationCode));
    }
}
