package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.response.course.CourseCompletionResponse;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.PackageEnrollment;
import fu.sep490.g23.backend.entity.User;

public interface CourseProgressService {

    PackageEnrollment refreshEnrollmentProgress(PackageEnrollment enrollment, OnlineCourse course, User student);
    CourseCompletionResponse buildCompletionResponse(PackageEnrollment enrollment, OnlineCourse course, User student);
}
