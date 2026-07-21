package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.request.course.CreateCourseVersionRequest;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCoursePreviewResponse;
import fu.sap490.g23.backend.dto.response.course.OnlineCourseVersionResponse;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;

import java.util.List;

public interface OnlineCourseVersionService {
    List<OnlineCourseVersionResponse> getVersions(Long courseId, String actorEmail);

    List<OnlineCourseVersionResponse> getPendingReviews(String actorEmail);

    OnlineCourseVersionResponse getVersion(Long courseId, Long versionId, String actorEmail);

    OnlineCoursePreviewResponse getVersionPreview(Long courseId, Long versionId, String actorEmail);

    OnlineCourseVersionResponse createDraft(Long courseId, CreateCourseVersionRequest request, String actorEmail);

    OnlineCourseVersionResponse submitForReview(Long courseId, Long versionId, String actorEmail);

    OnlineCourseVersionResponse publish(Long courseId, Long versionId, String actorEmail);

    OnlineCourseVersionResponse reject(Long courseId, Long versionId, String reviewNote, String actorEmail);

    void assertEditableDraft(OnlineCourse course, String actorEmail);

    void synchronizeDraftSnapshot(OnlineCourse course);

    OnlineCourseVersion requirePublishedVersion(OnlineCourse course);

    OnlineCourseResponse readEnrollmentSnapshot(PackageEnrollment enrollment, OnlineCourse liveCourse);

    OnlineCourseResponse readPublishedSnapshot(OnlineCourse course, boolean includeLessonContent);

    List<Long> getEnrollmentAssessmentIds(PackageEnrollment enrollment);

    void assertAssessmentBelongsToEnrollment(PackageEnrollment enrollment, Long assessmentId);

    void assertLessonBelongsToEnrollment(PackageEnrollment enrollment, Long lessonId);

    void assertLessonProgressTransitionAllowed(PackageEnrollment enrollment, Long lessonId, boolean completed);

    void assertLessonCanBeRemoved(OnlineCourse course, Long lessonId);
}
