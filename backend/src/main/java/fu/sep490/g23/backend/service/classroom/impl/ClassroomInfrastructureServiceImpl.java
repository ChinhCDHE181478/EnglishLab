package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.dto.request.classroom.UpsertRoomRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomRoomDetailResponse;
import fu.sep490.g23.backend.dto.request.classroom.UpsertCampusRequest;
import fu.sep490.g23.backend.repository.classroom.ClassroomRoomRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomCampusRepository;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomCampusResponse;

import fu.sep490.g23.backend.entity.classroom.ClassroomCampus;
import fu.sep490.g23.backend.entity.classroom.ClassroomRoom;
import fu.sep490.g23.backend.service.classroom.ClassroomInfrastructureService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomInfrastructureServiceImpl implements ClassroomInfrastructureService {

    private final ClassroomCampusRepository campusRepository;
    private final ClassroomRoomRepository roomRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomCampusResponse> listCampuses() {
        return campusRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toCampusResponse)
                .toList();
    }

    @Override
    public ClassroomCampusResponse createCampus(UpsertCampusRequest request) {
        ClassroomCampus campus = ClassroomCampus.builder()
                .name(request.getName().trim())
                .address(request.getAddress())
                .note(request.getNote())
                .active(request.getActive() == null || request.getActive())
                .build();
        return toCampusResponse(campusRepository.save(campus));
    }

    @Override
    public ClassroomCampusResponse updateCampus(Long id, UpsertCampusRequest request) {
        ClassroomCampus campus = campusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ sở."));
        campus.setName(request.getName().trim());
        campus.setAddress(request.getAddress());
        campus.setNote(request.getNote());
        if (request.getActive() != null) {
            campus.setActive(request.getActive());
        }
        return toCampusResponse(campusRepository.save(campus));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomRoomDetailResponse> listRooms(Long campusId) {
        List<ClassroomRoom> rooms = campusId == null
                ? roomRepository.findByActiveTrueOrderByNameAsc()
                : roomRepository.findByCampusIdAndActiveTrueOrderByNameAsc(campusId);
        return rooms.stream().map(this::toRoomResponse).toList();
    }

    @Override
    public ClassroomRoomDetailResponse createRoom(UpsertRoomRequest request) {
        ClassroomRoom room = ClassroomRoom.builder()
                .name(request.getName().trim())
                .capacity(request.getCapacity())
                .active(request.getActive() == null || request.getActive())
                .campus(resolveCampus(request.getCampusId()))
                .build();
        return toRoomResponse(roomRepository.save(room));
    }

    @Override
    public ClassroomRoomDetailResponse updateRoom(Long id, UpsertRoomRequest request) {
        ClassroomRoom room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng học."));
        room.setName(request.getName().trim());
        room.setCapacity(request.getCapacity());
        if (request.getActive() != null) {
            room.setActive(request.getActive());
        }
        room.setCampus(resolveCampus(request.getCampusId()));
        return toRoomResponse(roomRepository.save(room));
    }

    private ClassroomCampus resolveCampus(Long campusId) {
        if (campusId == null) {
            return null;
        }
        return campusRepository.findById(campusId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy cơ sở."));
    }

    private ClassroomCampusResponse toCampusResponse(ClassroomCampus campus) {
        long roomCount = roomRepository.findByCampusIdAndActiveTrueOrderByNameAsc(campus.getId()).size();
        return ClassroomCampusResponse.builder()
                .id(campus.getId())
                .name(campus.getName())
                .address(campus.getAddress())
                .note(campus.getNote())
                .active(campus.isActive())
                .roomCount(roomCount)
                .build();
    }

    private ClassroomRoomDetailResponse toRoomResponse(ClassroomRoom room) {
        return ClassroomRoomDetailResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .active(room.isActive())
                .campusId(room.getCampus() == null ? null : room.getCampus().getId())
                .campusName(room.getCampus() == null ? null : room.getCampus().getName())
                .build();
    }

}
