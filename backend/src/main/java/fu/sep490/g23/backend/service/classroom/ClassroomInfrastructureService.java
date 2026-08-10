package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.*;
import fu.sap490.g23.backend.dto.response.classroom.*;

import java.util.List;

public interface ClassroomInfrastructureService {
    List<ClassroomCampusResponse> listCampuses();
    ClassroomCampusResponse createCampus(UpsertCampusRequest request);
    ClassroomCampusResponse updateCampus(Long id, UpsertCampusRequest request);
    List<ClassroomRoomDetailResponse> listRooms(Long campusId);
    ClassroomRoomDetailResponse createRoom(UpsertRoomRequest request);
    ClassroomRoomDetailResponse updateRoom(Long id, UpsertRoomRequest request);
}
