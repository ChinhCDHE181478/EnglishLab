package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.request.assessment.ContentManagerCourseAssessmentRequest;
import fu.sep490.g23.backend.dto.request.course.OnlineCourseRequest;
import fu.sep490.g23.backend.dto.request.course.ReorderLessonsRequest;
import fu.sep490.g23.backend.dto.request.course.ReorderModulesRequest;
import fu.sep490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sep490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import fu.sep490.g23.backend.dto.response.course.BunnyVideoUploadResponse;
import fu.sep490.g23.backend.dto.response.course.CourseCertificateResponse;
import fu.sep490.g23.backend.dto.response.course.CourseCompletionResponse;
import fu.sep490.g23.backend.dto.response.course.CourseStatsResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCoursePreviewResponse;
import fu.sep490.g23.backend.dto.response.course.LessonResponse;
import fu.sep490.g23.backend.dto.response.course.ModuleResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseEnrollmentResponse;
import fu.sep490.g23.backend.dto.response.course.VocabularyTermResponse;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationContext;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.enums.VocabularyProgressStatus;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface OnlineCourseService {
    Page<OnlineCourseResponse> getPublicCourses(String keyword, String category, Double currentBand, Double targetBand, Integer targetScore, AssessmentSkill skill, String promotion, Pageable pageable);
    OnlineCourseResponse getPublicCourse(String slugOrId);
    CourseCertificateResponse verifyCourseCertificate(String verificationCode);
    Page<OnlineCourseResponse> getManagerCourses(String keyword, String category, CourseLevel level, PackageStatus status, Set<Long> excludedIds, Pageable pageable);
    OnlineCourseResponse getManagerCourse(String slugOrId);
    OnlineCoursePreviewResponse getManagerCoursePreview(String slugOrId);
    List<ModuleResponse> reorderModules(Long courseId, ReorderModulesRequest request, String actorEmail);
    List<LessonResponse> reorderLessons(Long courseId, Long moduleId, ReorderLessonsRequest request, String actorEmail);
    List<CourseAssessmentResponse> getManagerCourseAssessments(Long courseId);
    List<AssessmentRubricResponse> getManagerAssessmentRubrics();
    List<CourseAssessmentResponse> saveManagerCourseAssessments(Long courseId, List<ContentManagerCourseAssessmentRequest> requests);
    List<CourseAssessmentResponse> saveManagerCourseAssessments(Long courseId, List<ContentManagerCourseAssessmentRequest> requests, String actorEmail);
    CourseStatsResponse getStats();
    OnlineCourseResponse createCourse(OnlineCourseRequest request, String creatorEmail);
    OnlineCourseResponse updateCourse(Long id, OnlineCourseRequest request);
    OnlineCourseResponse updateCourse(Long id, OnlineCourseRequest request, String actorEmail);
    OnlineCourseResponse publishCourse(Long id, String actorEmail);
    OnlineCourseResponse archiveCourse(Long id);
    void deleteCourse(Long id);
    OnlineCourseResponse registerCourse(Long courseId, String studentEmail);
    OnlineCourseResponse getEnrolledCourse(Long courseId, String studentEmail);
    OnlineCourseResponse activatePaidCourse(Long courseId, String studentEmail);

    /** Hủy quyền học sau khi hoàn tiền đơn PayOS khóa học. */
    void revokePaidCourseAccess(Long courseId, String studentEmail);

    List<OnlineCourseEnrollmentResponse> getMyEnrollments(String studentEmail);
    List<OnlineCourseResponse> getRecommendedCourses(String studentEmail);
    /** Placement ranking: score published courses, then mix weak-skill + level-fit shortlist. */
    List<OnlineCourseResponse> recommendCourses(User student, PlacementRecommendationContext context);
    CourseCompletionResponse getCourseCompletion(Long courseId, String studentEmail);
    CourseCertificateResponse getCourseCertificate(Long courseId, String studentEmail);

    OnlineCourseEnrollmentResponse updateLessonProgress(Long courseId, Long lessonId, boolean completed, String studentEmail);
    List<VocabularyTermResponse> getVocabularyTerms(Long courseId, String studentEmail);
    VocabularyTermResponse updateVocabularyProgress(Long courseId, String termKey, VocabularyProgressStatus status, Boolean starred, Boolean reviewed, Boolean correct, String studentEmail);
    BunnyVideoUploadResponse uploadLessonVideo(Long courseId, Long lessonId, String title, MultipartFile file);
    BunnyVideoUploadResponse uploadLessonVideo(Long courseId, Long lessonId, String title, MultipartFile file, String actorEmail);
    OnlineCourseResponse refreshLessonTranscript(Long courseId, Long lessonId);
    OnlineCourseResponse refreshLessonTranscript(Long courseId, Long lessonId, String actorEmail);
}
