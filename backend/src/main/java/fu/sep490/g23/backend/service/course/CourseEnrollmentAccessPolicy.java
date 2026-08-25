package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;

public interface CourseEnrollmentAccessPolicy {

    boolean hasLearningAccess(OnlineCourseEnrollment enrollment);

    boolean hasAssessmentAccess(OnlineCourseEnrollment enrollment);

    OnlineCourseEnrollment requireLearningAccess(User student, OnlineCourse course);

    OnlineCourseEnrollment requireAssessmentAccess(User student, OnlineCourse course);

    /**
     * Reactivates a cancelled enrollment and persists the change.
     */
    OnlineCourseEnrollment reactivateCancelledEnrollment(OnlineCourseEnrollment enrollment);
}
