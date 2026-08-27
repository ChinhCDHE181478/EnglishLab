package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.InstructorLedCourseRequest;
import fu.sep490.g23.backend.dto.response.classroom.InstructorLedCourseResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;

import java.util.List;

public interface InstructorLedCourseCatalogService {
    List<InstructorLedCourseResponse> listPrograms(ClassroomDeliveryMode deliveryMode);

    List<InstructorLedCourseResponse> listPublishedPrograms(ClassroomDeliveryMode deliveryMode);

    InstructorLedCourseResponse getPublishedProgram(String slugOrId);

    InstructorLedCourseResponse getProgram(Long id);

    InstructorLedCourseResponse createProgram(InstructorLedCourseRequest request);

    InstructorLedCourseResponse updateProgram(Long id, InstructorLedCourseRequest request);

    InstructorLedCourseResponse cloneProgram(Long id);

    void archiveProgram(Long id);
}
