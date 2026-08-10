package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.UpdateClassroomProgramRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;

import java.util.List;

public interface ClassroomProgramService {
    List<ClassroomOfferingResponse> listPrograms(ClassroomDeliveryMode deliveryMode);

    ClassroomOfferingResponse updateProgramProfile(Long offeringId, UpdateClassroomProgramRequest request);
}
