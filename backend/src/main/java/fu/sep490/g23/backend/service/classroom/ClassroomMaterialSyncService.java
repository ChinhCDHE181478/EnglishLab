package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;

public interface ClassroomMaterialSyncService {

    void synchronizeMandatoryMaterials(ClassroomOffering offering, User actor);
}
