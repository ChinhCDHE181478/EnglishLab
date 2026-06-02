package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.OnlineCourseRequest;
import fu.sap490.g23.backend.dto.response.course.CourseStatsResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.dto.response.course.PackageEnrollmentResponse;
import fu.sap490.g23.backend.entity.course.CourseCategoryCode;
import fu.sap490.g23.backend.entity.course.PackageStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OnlineCourseService {
    Page<OnlineCourseResponse> getPublicCourses(String keyword, CourseCategoryCode category, Pageable pageable);
    OnlineCourseResponse getPublicCourse(String slugOrId);
    Page<OnlineCourseResponse> getManagerCourses(String keyword, CourseCategoryCode category, PackageStatus status, Pageable pageable);
    CourseStatsResponse getStats();
    OnlineCourseResponse createCourse(OnlineCourseRequest request, String creatorEmail);
    OnlineCourseResponse updateCourse(Long id, OnlineCourseRequest request);
    OnlineCourseResponse publishCourse(Long id);
    OnlineCourseResponse archiveCourse(Long id);
    void deleteCourse(Long id);
    OnlineCourseResponse registerCourse(Long courseId, String studentEmail);
    List<PackageEnrollmentResponse> getMyEnrollments(String studentEmail);
    PackageEnrollmentResponse updateLessonProgress(Long courseId, Long lessonId, boolean completed, String studentEmail);
}
