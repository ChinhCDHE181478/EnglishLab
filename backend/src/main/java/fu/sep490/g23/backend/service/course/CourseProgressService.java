package fu.sap490.g23.backend.service.course;

import fu.sap490.g23.backend.dto.response.course.CourseCompletionResponse;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.entity.User;

public interface CourseProgressService {

    PackageEnrollment refreshEnrollmentProgress(PackageEnrollment enrollment, OnlineCourse course, User student);
    CourseCompletionResponse buildCompletionResponse(PackageEnrollment enrollment, OnlineCourse course, User student);
}
