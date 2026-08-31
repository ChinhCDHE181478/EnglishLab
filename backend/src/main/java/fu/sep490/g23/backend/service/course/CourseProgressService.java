package fu.sep490.g23.backend.service.course;

import fu.sep490.g23.backend.dto.response.course.CourseCompletionResponse;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.User;

public interface CourseProgressService {

    OnlineCourseEnrollment refreshEnrollmentProgress(OnlineCourseEnrollment enrollment, OnlineCourse course, User student);
    CourseCompletionResponse buildCompletionResponse(OnlineCourseEnrollment enrollment, OnlineCourse course, User student);
}
