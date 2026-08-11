package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.TrainingProgramRequest;
import fu.sep490.g23.backend.dto.response.classroom.TrainingProgramResponse;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;

import java.util.List;

public interface TrainingProgramService {
    List<TrainingProgramResponse> listPrograms(ClassroomDeliveryMode deliveryMode);

    List<TrainingProgramResponse> listPublishedPrograms(ClassroomDeliveryMode deliveryMode);

    TrainingProgramResponse getPublishedProgram(String slugOrId);

    TrainingProgramResponse getProgram(Long id);

    TrainingProgramResponse createProgram(TrainingProgramRequest request);

    TrainingProgramResponse updateProgram(Long id, TrainingProgramRequest request);

    TrainingProgramResponse cloneProgram(Long id);

    void archiveProgram(Long id);
}
