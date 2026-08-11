package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.PackageEnrollment;

public interface CourseEnrollmentAccessPolicy {

    boolean hasLearningAccess(PackageEnrollment enrollment);

    boolean hasAssessmentAccess(PackageEnrollment enrollment);

    PackageEnrollment requireLearningAccess(User student, OnlineCourse course);

    PackageEnrollment requireAssessmentAccess(User student, OnlineCourse course);

    /**
     * Reactivates a cancelled enrollment and persists the change.
     */
    PackageEnrollment reactivateCancelledEnrollment(PackageEnrollment enrollment);
}
