package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.OnlineCourseRequest;
import fu.sap490.g23.backend.dto.response.course.BunnyVideoUploadResponse;
import fu.sap490.g23.backend.dto.response.course.CourseStatsResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.dto.response.course.PackageEnrollmentResponse;
import fu.sap490.g23.backend.dto.response.course.VocabularyTermResponse;
import fu.sap490.g23.backend.entity.course.CourseCategoryCode;
import fu.sap490.g23.backend.entity.course.PackageStatus;
import fu.sap490.g23.backend.entity.course.VocabularyProgressStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OnlineCourseService {
    Page<OnlineCourseResponse> getPublicCourses(String keyword, CourseCategoryCode category, Pageable pageable);
    OnlineCourseResponse getPublicCourse(String slugOrId);
    Page<OnlineCourseResponse> getManagerCourses(String keyword, CourseCategoryCode category, PackageStatus status, Pageable pageable);
    OnlineCourseResponse getManagerCourse(String slugOrId);
    CourseStatsResponse getStats();
    OnlineCourseResponse createCourse(OnlineCourseRequest request, String creatorEmail);
    OnlineCourseResponse updateCourse(Long id, OnlineCourseRequest request);
    OnlineCourseResponse publishCourse(Long id);
    OnlineCourseResponse archiveCourse(Long id);
    void deleteCourse(Long id);
    OnlineCourseResponse registerCourse(Long courseId, String studentEmail);
    List<PackageEnrollmentResponse> getMyEnrollments(String studentEmail);
    PackageEnrollmentResponse updateLessonProgress(Long courseId, Long lessonId, boolean completed, String studentEmail);
    List<VocabularyTermResponse> getVocabularyTerms(Long courseId, String studentEmail);
    VocabularyTermResponse updateVocabularyProgress(Long courseId, String termKey, VocabularyProgressStatus status, Boolean starred, Boolean reviewed, Boolean correct, String studentEmail);
    BunnyVideoUploadResponse uploadLessonVideo(Long courseId, Long lessonId, String title, MultipartFile file);
}
