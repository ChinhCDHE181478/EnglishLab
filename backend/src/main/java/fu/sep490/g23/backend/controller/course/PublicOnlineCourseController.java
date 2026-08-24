package fu.sep490.g23.backend.controller.course;

import fu.sep490.g23.backend.dto.response.course.CourseCertificateResponse;
import fu.sep490.g23.backend.dto.response.course.CourseCategoryResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import fu.sep490.g23.backend.service.course.CourseCategoryManagementService;
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

import java.util.List;

@RestController
@RequestMapping("/api/online-courses")
@RequiredArgsConstructor
public class PublicOnlineCourseController {

    private final OnlineCourseService onlineCourseService;
    private final CourseCategoryManagementService categoryService;

    @GetMapping
    public ResponseEntity<Page<OnlineCourseResponse>> getCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Double currentBand,
            @RequestParam(required = false) Double targetBand,
            @RequestParam(required = false) Integer targetScore,
            @RequestParam(required = false) AssessmentSkill skill,
            @RequestParam(required = false) String promotion,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return ResponseEntity.ok(onlineCourseService.getPublicCourses(keyword, category, currentBand, targetBand, targetScore, skill, promotion, pageable));
    }

    @GetMapping("/categories")
    public ResponseEntity<List<CourseCategoryResponse>> getCategories() {
        return ResponseEntity.ok(categoryService.getActiveCategories());
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
