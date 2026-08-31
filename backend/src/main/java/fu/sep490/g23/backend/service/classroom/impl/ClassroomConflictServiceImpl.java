package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ConflictType;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.repository.classroom.RoomRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;

import fu.sep490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictItemResponse;
import fu.sep490.g23.backend.exception.ClassroomConflictException;
import fu.sep490.g23.backend.service.classroom.ClassroomConflictService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class ClassroomConflictServiceImpl implements ClassroomConflictService {

    private static final Set<ClassroomSessionStatus> ACTIVE_SESSION_STATUSES = EnumSet.of(
            ClassroomSessionStatus.SCHEDULED,
            ClassroomSessionStatus.OPEN,
            ClassroomSessionStatus.IN_PROGRESS
    );

    private static final Set<ClassroomRegistrationStatus> OCCUPIES_CLASS_SLOT = ClassroomRegistrationSupport.OCCUPIES_CLASS_SLOT;
    private static final Set<ClassroomRegistrationStatus> ACTIVE_REGISTRATIONS = ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS;

    private final ClassScheduleRepository sessionRepository;
    private final ClassEnrollmentRepository enrollmentRepository;
    private final ClassSectionRepository offeringRepository;
    private final RoomRepository roomRepository;

    public ConflictCheckResultResponse check(ConflictCheckRequest request) {
        List<ConflictItemResponse> conflicts = new ArrayList<>();

        if (request.getSessionId() != null && Boolean.TRUE.equals(request.getCheckSessionLocked())) {
            sessionRepository.findById(request.getSessionId()).ifPresent(session -> {
                if (session.isImmutable()) {
                    conflicts.add(item(
                            ConflictType.SESSION_LOCKED,
                            "Buổi học đã hoàn thành hoặc đã khóa nên không thể đổi lịch.",
                            Map.of("sessionId", session.getId())
                    ));
                }
            });
        }

        if (request.getSessionDate() != null && request.getStartTime() != null && request.getEndTime() != null) {
            if (request.getTeacherId() != null) {
                sessionRepository.findTeacherConflicts(
                        request.getTeacherId(),
                        request.getSessionDate(),
                        request.getStartTime(),
                        request.getEndTime(),
                        ACTIVE_SESSION_STATUSES,
                        request.getExcludeSessionId()
                ).forEach(session -> conflicts.add(item(
                        ConflictType.TEACHER_SCHEDULE,
                        "Giáo viên đã có lịch dạy khác trong khung giờ này.",
                        Map.of(
                                "teacherId", request.getTeacherId(),
                                "sessionId", session.getId(),
                                "classroomTitle", session.getClassSection().getTitle(),
                                "overlapStart", session.getStartDateTime().toString(),
                                "overlapEnd", session.getEndDateTime().toString()
                        )
                )));
            }

            if (request.getRoomId() != null) {
                sessionRepository.findRoomConflicts(
                        request.getRoomId(),
                        request.getSessionDate(),
                        request.getStartTime(),
                        request.getEndTime(),
                        ACTIVE_SESSION_STATUSES,
                        request.getExcludeSessionId()
                ).forEach(session -> {
                    String roomName = session.getRoom() != null ? session.getRoom().getName() : "";
                    conflicts.add(item(
                            ConflictType.ROOM_SCHEDULE,
                            "Phòng học đã được sử dụng trong khung giờ này.",
                            Map.of(
                                    "roomId", request.getRoomId(),
                                    "roomName", roomName,
                                    "sessionId", session.getId(),
                                    "classroomTitle", session.getClassSection().getTitle()
                            )
                    ));
                });
            }

            List<Long> learnerIds = resolveLearnerIds(request);
            for (Long learnerId : learnerIds) {
                sessionRepository.findLearnerConflicts(
                        learnerId,
                        request.getSessionDate(),
                        request.getStartTime(),
                        request.getEndTime(),
                        ACTIVE_SESSION_STATUSES,
                        request.getExcludeSessionId()
                ).forEach(session -> conflicts.add(item(
                        ConflictType.LEARNER_SCHEDULE,
                        "Học viên bị trùng lịch với lớp khác.",
                        Map.of(
                                "learnerId", learnerId,
                                "sessionId", session.getId(),
                                "classroomTitle", session.getClassSection().getTitle()
                        )
                )));
            }
        }

        if (isCapacityCheckEnabled(request) && request.getTargetClassSectionId() != null && request.getLearnerIds() != null) {
            offeringRepository.findById(request.getTargetClassSectionId()).ifPresent(target -> {
                long current = enrollmentRepository.countByOfferingAndRegistrationStatuses(
                        target.getId(),
                        OCCUPIES_CLASS_SLOT
                );
                int incoming = request.getLearnerIds().size();
                if (current + incoming > target.getCapacity()) {
                    conflicts.add(item(
                            ConflictType.CLASS_CAPACITY,
                            "Lớp đích đã đủ sĩ số.",
                            Map.of(
                                    "targetClassroomId", target.getId(),
                                    "capacity", target.getCapacity(),
                                    "currentCount", current,
                                    "incomingCount", incoming
                            )
                    ));
                }
            });
        }

        if (isCapacityCheckEnabled(request) && request.getClassSectionId() != null && request.getLearnerIds() != null && request.getTargetClassSectionId() == null) {
            offeringRepository.findById(request.getClassSectionId()).ifPresent(offering -> {
                long current = enrollmentRepository.countByOfferingAndRegistrationStatuses(offering.getId(), OCCUPIES_CLASS_SLOT);
                for (Long learnerId : request.getLearnerIds()) {
                    boolean alreadyEnrolled = enrollmentRepository.existsByStudentIdAndClassSectionIdAndRegistrationStatusIn(
                            learnerId, offering.getId(), ACTIVE_REGISTRATIONS
                    );
                    if (alreadyEnrolled) {
                        conflicts.add(item(
                                ConflictType.DUPLICATE_ENROLLMENT,
                                "Học viên đã được xếp vào lớp này.",
                                Map.of("learnerId", learnerId, "classroomId", offering.getId())
                        ));
                    }
                }
                if (current + request.getLearnerIds().size() > offering.getCapacity()) {
                    conflicts.add(item(
                            ConflictType.CLASS_CAPACITY,
                            "Lớp đã đủ sĩ số.",
                            Map.of("classroomId", offering.getId(), "capacity", offering.getCapacity())
                    ));
                }
            });
        }

        boolean hasBlocking = !conflicts.isEmpty();
        return ConflictCheckResultResponse.builder()
                .hasBlockingConflict(hasBlocking)
                .canOverride(hasBlocking)
                .conflicts(conflicts)
                .build();
    }

    public void assertNoBlockingConflict(ConflictCheckRequest request) {
        ConflictCheckResultResponse result = check(request);
        if (result.isHasBlockingConflict()) {
            throw new ClassroomConflictException("Phát hiện xung đột lịch học.", result);
        }
    }

    private boolean isCapacityCheckEnabled(ConflictCheckRequest request) {
        return !Boolean.FALSE.equals(request.getCheckCapacity());
    }

    private List<Long> resolveLearnerIds(ConflictCheckRequest request) {
        if (request.getLearnerIds() != null && !request.getLearnerIds().isEmpty()) {
            return request.getLearnerIds();
        }
        if (request.getClassSectionId() == null) {
            return List.of();
        }
        return enrollmentRepository.findByClassSectionIdAndRegistrationStatusIn(
                request.getClassSectionId(),
                OCCUPIES_CLASS_SLOT
        ).stream().map(e -> e.getStudent().getId()).toList();
    }

    private ConflictItemResponse item(ConflictType type, String message, Map<String, Object> details) {
        return ConflictItemResponse.builder()
                .type(type)
                .message(message)
                .details(details)
                .build();
    }
}
