package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.response.classroom.InstructorLedCourseResponse;
import java.util.List;

public interface InstructorLedCourseCatalogService {
    List<InstructorLedCourseResponse> listInstructorLedCourses();

    List<InstructorLedCourseResponse> listPublishedInstructorLedCourses();

    InstructorLedCourseResponse getPublishedInstructorLedCourse(String idOrCode);

    InstructorLedCourseResponse getInstructorLedCourse(Long id);

}
