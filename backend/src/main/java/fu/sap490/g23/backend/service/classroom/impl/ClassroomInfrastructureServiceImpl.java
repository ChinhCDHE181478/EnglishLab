package fu.sap490.g23.backend.service.classroom.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sap490.g23.backend.dto.request.classroom.*;
import fu.sap490.g23.backend.dto.response.classroom.*;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.*;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.*;
import fu.sap490.g23.backend.service.classroom.ClassroomInfrastructureService;
import fu.sap490.g23.backend.service.classroom.ClassroomOfferingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomInfrastructureServiceImpl implements ClassroomInfrastructureService {

    private final ClassroomCampusRepository campusRepository;
    private final ClassroomRoomRepository roomRepository;
    private final ClassroomSessionTemplateRepository templateRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final ClassroomOfferingService offeringService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomSessionTemplateResponse> listSessionTemplates() {
        return templateRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::toTemplateResponse)
                .toList();
    }

    @Override
    public ClassroomSessionTemplateResponse createSessionTemplate(UpsertSessionTemplateRequest request, String creatorEmail) {
        validateSlotsJson(request.getSlotsJson());
        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
        ClassroomSessionTemplate template = ClassroomSessionTemplate.builder()
                .name(request.getName().trim())
                .slotsJson(request.getSlotsJson())
                .description(request.getDescription())
                .teacherGuide(request.getTeacherGuide())
                .interactionActivities(request.getInteractionActivities())
                .postSessionHomework(request.getPostSessionHomework())
                .defaultDurationMinutes(request.getDefaultDurationMinutes())
                .active(request.getActive() == null || request.getActive())
                .createdBy(creator)
                .build();
        return toTemplateResponse(templateRepository.save(template));
    }

    @Override
    public ClassroomSessionTemplateResponse updateSessionTemplate(Long id, UpsertSessionTemplateRequest request) {
        ClassroomSessionTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu lịch."));
        if (request.getSlotsJson() != null) {
            validateSlotsJson(request.getSlotsJson());
            template.setSlotsJson(request.getSlotsJson());
        }
        template.setName(request.getName().trim());
        template.setDescription(request.getDescription());
        template.setTeacherGuide(request.getTeacherGuide());
        template.setInteractionActivities(request.getInteractionActivities());
        template.setPostSessionHomework(request.getPostSessionHomework());
        template.setDefaultDurationMinutes(request.getDefaultDurationMinutes());
        if (request.getActive() != null) {
            template.setActive(request.getActive());
        }
        return toTemplateResponse(templateRepository.save(template));
    }

    @Override
    public List<ClassroomSessionResponse> generateSessionsFromTemplate(Long offeringId, GenerateSessionsFromTemplateRequest request) {
        offeringRepository.findById(offeringId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp học."));
        ClassroomSessionTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu lịch."));
        List<Map<String, Object>> slots = parseSlots(template.getSlotsJson());
        if (slots.isEmpty()) {
            throw new RuntimeException("Mẫu lịch không có khung giờ hợp lệ.");
        }

        List<ClassroomSessionResponse> created = new ArrayList<>();
        LocalDate cursor = request.getStartDate();
        int weeks = request.getWeeks();
        for (int week = 0; week < weeks; week++) {
            for (Map<String, Object> slot : slots) {
                int dayOfWeek = Integer.parseInt(String.valueOf(slot.get("dayOfWeek")));
                LocalDate sessionDate = cursor.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.of(dayOfWeek)));
                if (sessionDate.isBefore(request.getStartDate())) {
                    sessionDate = sessionDate.plusWeeks(1);
                }
                if (week > 0) {
                    sessionDate = sessionDate.plusWeeks(week);
                }
                CreateClassroomSessionRequest sessionRequest = CreateClassroomSessionRequest.builder()
                        .sessionDate(sessionDate)
                        .startTime(LocalTime.parse(String.valueOf(slot.get("startTime"))))
                        .endTime(LocalTime.parse(String.valueOf(slot.get("endTime"))))
                        .roomId(slot.get("roomId") == null ? null : Long.valueOf(String.valueOf(slot.get("roomId"))))
                        .teacherId(slot.get("teacherId") == null ? null : Long.valueOf(String.valueOf(slot.get("teacherId"))))
                        .build();
                try {
                    created.add(offeringService.createSession(offeringId, sessionRequest));
                } catch (RuntimeException ex) {
                    throw new RuntimeException("Không thể tạo buổi học ngày " + sessionDate + ": " + ex.getMessage());
                }
            }
        }
        return created;
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

    private ClassroomSessionTemplateResponse toTemplateResponse(ClassroomSessionTemplate template) {
        return ClassroomSessionTemplateResponse.builder()
                .id(template.getId())
                .name(template.getName())
                .slotsJson(template.getSlotsJson())
                .description(template.getDescription())
                .teacherGuide(template.getTeacherGuide())
                .interactionActivities(template.getInteractionActivities())
                .postSessionHomework(template.getPostSessionHomework())
                .defaultDurationMinutes(template.getDefaultDurationMinutes())
                .active(template.isActive())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }

    private void validateSlotsJson(String slotsJson) {
        List<Map<String, Object>> slots = parseSlots(slotsJson);
        if (slots.isEmpty()) {
            throw new RuntimeException("Cấu hình khung giờ phải là mảng JSON hợp lệ.");
        }
        for (Map<String, Object> slot : slots) {
            if (!slot.containsKey("dayOfWeek") || !slot.containsKey("startTime") || !slot.containsKey("endTime")) {
                throw new RuntimeException("Mỗi khung giờ cần dayOfWeek, startTime, endTime.");
            }
        }
    }

    private List<Map<String, Object>> parseSlots(String slotsJson) {
        try {
            return objectMapper.readValue(slotsJson, new TypeReference<>() {});
        } catch (Exception ex) {
            throw new RuntimeException("Cấu hình khung giờ không hợp lệ.");
        }
    }
}
