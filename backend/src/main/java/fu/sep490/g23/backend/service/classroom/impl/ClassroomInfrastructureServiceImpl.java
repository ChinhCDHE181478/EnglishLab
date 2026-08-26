package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.dto.request.classroom.UpsertRoomRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomRoomDetailResponse;
import fu.sep490.g23.backend.repository.classroom.RoomRepository;
import fu.sep490.g23.backend.entity.classroom.Room;
import fu.sep490.g23.backend.service.classroom.ClassroomInfrastructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomInfrastructureServiceImpl implements ClassroomInfrastructureService {

    private final RoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomRoomDetailResponse> listRooms() {
        return roomRepository.findAll().stream().map(this::toRoomResponse).toList();
    }

    @Override
    public ClassroomRoomDetailResponse createRoom(UpsertRoomRequest request) {
        Room room = Room.builder()
                .name(request.getName().trim())
                .capacity(request.getCapacity())
                .active(request.getActive() == null || request.getActive())
                .locationName(trimToNull(request.getLocationName()))
                .locationAddress(trimToNull(request.getLocationAddress()))
                .build();
        return toRoomResponse(roomRepository.save(room));
    }

    @Override
    public ClassroomRoomDetailResponse updateRoom(Long id, UpsertRoomRequest request) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng học."));
        room.setName(request.getName().trim());
        room.setCapacity(request.getCapacity());
        if (request.getActive() != null) {
            room.setActive(request.getActive());
        }
        room.setLocationName(trimToNull(request.getLocationName()));
        room.setLocationAddress(trimToNull(request.getLocationAddress()));
        return toRoomResponse(roomRepository.save(room));
    }

    private ClassroomRoomDetailResponse toRoomResponse(Room room) {
        return ClassroomRoomDetailResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .active(room.isActive())
                .locationName(room.getLocationName())
                .locationAddress(room.getLocationAddress())
                .build();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
