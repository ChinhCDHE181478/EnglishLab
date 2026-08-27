package fu.sep490.g23.backend.service.classroom;
import fu.sep490.g23.backend.dto.request.classroom.UpsertRoomRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomRoomDetailResponse;

import fu.sep490.g23.backend.dto.request.classroom.*;
import fu.sep490.g23.backend.dto.response.classroom.*;

import java.util.List;

public interface ClassroomInfrastructureService {
    List<ClassroomRoomDetailResponse> listRooms();
    ClassroomRoomDetailResponse createRoom(UpsertRoomRequest request);
    ClassroomRoomDetailResponse updateRoom(Long id, UpsertRoomRequest request);
}
