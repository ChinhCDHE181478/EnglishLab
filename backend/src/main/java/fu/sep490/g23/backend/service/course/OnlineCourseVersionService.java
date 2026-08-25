package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.request.course.CreateCourseVersionRequest;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCoursePreviewResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseVersionResponse;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;

import java.util.List;

public interface OnlineCourseVersionService {
    List<OnlineCourseVersionResponse> getVersions(Long courseId, String actorEmail);

    OnlineCourseVersionResponse getVersion(Long courseId, Long versionId, String actorEmail);

    OnlineCoursePreviewResponse getVersionPreview(Long courseId, Long versionId, String actorEmail);

    OnlineCourseVersionResponse createDraft(Long courseId, CreateCourseVersionRequest request, String actorEmail);

    OnlineCourseVersionResponse publish(Long courseId, Long versionId, String actorEmail);

    void assertEditableDraft(OnlineCourse course, String actorEmail);

    void synchronizeDraftSnapshot(OnlineCourse course);

    OnlineCourseVersion requirePublishedVersion(OnlineCourse course);

    void refreshPublishedSnapshot(OnlineCourse course);

    OnlineCourseResponse readLatestPublishedForEnrollment(OnlineCourseEnrollment enrollment, OnlineCourse liveCourse);

    OnlineCourseResponse readPublishedSnapshot(OnlineCourse course, boolean includeLessonContent);

    List<Long> getLatestPublishedAssessmentIds(OnlineCourseEnrollment enrollment);

    List<Long> getProgressBaselineAssessmentIds(OnlineCourseEnrollment enrollment);

    void assertAssessmentBelongsToEnrollment(OnlineCourseEnrollment enrollment, Long assessmentId);

    void assertLessonBelongsToEnrollment(OnlineCourseEnrollment enrollment, Long lessonId);

    void assertLessonProgressTransitionAllowed(OnlineCourseEnrollment enrollment, Long lessonId, boolean completed);

    boolean isAssessmentReferencedByPublishedHistory(OnlineCourse course, Long assessmentId);

    void assertLessonCanBeRemoved(OnlineCourse course, Long lessonId);
}
