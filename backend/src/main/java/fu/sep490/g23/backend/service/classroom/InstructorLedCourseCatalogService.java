package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.response.classroom.InstructorLedCourseResponse;
import java.util.List;

public interface InstructorLedCourseCatalogService {
    List<InstructorLedCourseResponse> listPrograms();

    List<InstructorLedCourseResponse> listPublishedPrograms();

    InstructorLedCourseResponse getPublishedProgram(String idOrCode);

    InstructorLedCourseResponse getProgram(Long id);

}
