package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.service.classroom.*;

import fu.sap490.g23.backend.dto.response.classroom.AvailableRoomOptionResponse;
import fu.sap490.g23.backend.dto.response.classroom.AvailableTeacherOptionResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomRoom;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomRoomRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClassroomScheduleAvailabilityServiceImpl implements ClassroomScheduleAvailabilityService {

    private static final Set<ClassroomSessionStatus> ACTIVE_SESSION_STATUSES = EnumSet.of(
            ClassroomSessionStatus.SCHEDULED,
            ClassroomSessionStatus.OPEN,
            ClassroomSessionStatus.IN_PROGRESS,
            ClassroomSessionStatus.RESCHEDULED,
            ClassroomSessionStatus.MAKEUP
    );

    private final ClassroomRoomRepository roomRepository;
    private final ClassroomSessionRepository sessionRepository;
    private final UserRepository userRepository;

    public List<AvailableRoomOptionResponse> listAvailableRooms(
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            Long excludeSessionId
    ) {
        if (sessionDate == null || startTime == null || endTime == null) {
            return List.of();
        }

        return roomRepository.findByActiveTrue().stream()
                .filter(room -> sessionRepository.findRoomConflicts(
                        room.getId(),
                        sessionDate,
                        startTime,
                        endTime,
                        ACTIVE_SESSION_STATUSES,
                        excludeSessionId
                ).isEmpty())
                .map(this::toRoomOption)
                .toList();
    }

    public List<AvailableTeacherOptionResponse> listAvailableTeachers(
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime,
            Long excludeSessionId
    ) {
        if (sessionDate == null || startTime == null || endTime == null) {
            return List.of();
        }

        return userRepository.findDistinctByRoles_CodeIn(List.of(RoleEnum.TEACHER)).stream()
                .filter(teacher -> sessionRepository.findTeacherConflicts(
                        teacher.getId(),
                        sessionDate,
                        startTime,
                        endTime,
                        ACTIVE_SESSION_STATUSES,
                        excludeSessionId
                ).isEmpty())
                .map(this::toTeacherOption)
                .toList();
    }

    private AvailableRoomOptionResponse toRoomOption(ClassroomRoom room) {
        return AvailableRoomOptionResponse.builder()
                .id(room.getId())
                .name(room.getName())
                .capacity(room.getCapacity())
                .build();
    }

    private AvailableTeacherOptionResponse toTeacherOption(User teacher) {
        return AvailableTeacherOptionResponse.builder()
                .id(teacher.getId())
                .fullName(teacher.getFullName())
                .email(teacher.getEmail())
                .build();
    }
}
