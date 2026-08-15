package fu.sep490.g23.backend.service.classroom.impl;

import fu.sep490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomOfferingRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomProposalRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomSessionRequest;
import fu.sep490.g23.backend.dto.request.classroom.RejectClassroomProposalRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomProposalMemberResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomProposalResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomProposalAvailabilityResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomPickerOptionResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictItemResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.ClassroomProposal;
import fu.sep490.g23.backend.entity.classroom.ClassroomProposalMember;
import fu.sep490.g23.backend.entity.classroom.ClassroomRoom;
import fu.sep490.g23.backend.entity.classroom.EnrollmentRequest;
import fu.sep490.g23.backend.entity.classroom.TrainingProgram;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomApprovalStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ConflictType;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.curriculum.CurriculumSessionPlan;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.exception.ClassroomConflictException;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomProposalRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomRoomRepository;
import fu.sep490.g23.backend.repository.classroom.TrainingProgramRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumSessionPlanRepository;
import fu.sep490.g23.backend.security.TrainingRolePolicy;
import fu.sep490.g23.backend.service.classroom.ClassroomConflictService;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sep490.g23.backend.service.classroom.ClassroomProposalService;
import fu.sep490.g23.backend.service.classroom.ClassroomScheduleLockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ClassroomProposalServiceImpl implements ClassroomProposalService {
    private final ClassroomProposalRepository proposalRepository;
    private final TrainingProgramRepository trainingProgramRepository;
    private final ClassroomRoomRepository roomRepository;
    private final ClassroomOfferingRepository offeringRepository;
    private final UserRepository userRepository;
    private final ClassroomConflictService conflictService;
    private final ClassroomScheduleLockService scheduleLockService;
    private final ClassroomOfferingService classroomOfferingService;
    private final CurriculumSessionPlanRepository curriculumSessionPlanRepository;

    @Override
    public ClassroomProposalResponse create(CreateClassroomProposalRequest payload, String staffEmail) {
        validateProposalPayload(payload);
        User staff = requireStaff(staffEmail);
        TrainingProgram courseOffering = requirePublishedOffering(payload.getCourseOfferingId());
        ClassroomProposal proposal = ClassroomProposal.builder()
                .proposalCode("CP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .courseOffering(courseOffering)
                .deliveryType(courseOffering.getDeliveryMode())
                .placementLevel(null)
                .createdBy(staff)
                .approvalStatus(ClassroomApprovalStatus.DRAFT)
                .build();
        applyProposalFields(proposal, payload, 0);
        scheduleLockService.lockDates(sessionDates(proposal));
        assertNoScheduleConflicts(proposal, null);
        proposalRepository.save(proposal);
        return toResponse(proposal);
    }

    @Override
    public ClassroomProposalResponse update(
            Long proposalId,
            CreateClassroomProposalRequest payload,
            String staffEmail
    ) {
        validateProposalPayload(payload);
        User staff = requireStaff(staffEmail);
        ClassroomProposal proposal = requireProposal(proposalId);
        if (proposal.getApprovalStatus() != ClassroomApprovalStatus.DRAFT
                && proposal.getApprovalStatus() != ClassroomApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("Chỉ có thể sửa đề xuất nháp hoặc đã bị từ chối.");
        }
        if (!proposal.getCourseOffering().getId().equals(payload.getCourseOfferingId())) {
            throw new IllegalArgumentException("Không thể đổi khóa học của đề xuất đã tạo.");
        }
        proposal.setDeliveryType(proposal.getCourseOffering().getDeliveryMode());
        applyProposalFields(proposal, payload, 0);
        scheduleLockService.lockDates(sessionDates(proposal));
        assertNoScheduleConflicts(proposal, proposal.getId());
        proposal.setStaffNote(trimOrNull(payload.getNote()));
        proposal.setReviewedBy(null);
        proposal.setReviewedAt(null);
        proposal.setReviewNote(null);
        if (proposal.getApprovalStatus() == ClassroomApprovalStatus.REJECTED) {
            proposal.setApprovalStatus(ClassroomApprovalStatus.DRAFT);
        }
        return toResponse(proposalRepository.save(proposal));
    }

    @Override
    public ConflictCheckResultResponse validateSchedule(
            CreateClassroomProposalRequest payload,
            Long excludeProposalId,
            String staffEmail
    ) {
        validateProposalPayload(payload);
        requireStaff(staffEmail);
        TrainingProgram courseOffering = requirePublishedOffering(payload.getCourseOfferingId());
        ClassroomProposal proposal = ClassroomProposal.builder()
                .courseOffering(courseOffering)
                .deliveryType(courseOffering.getDeliveryMode())
                .approvalStatus(ClassroomApprovalStatus.DRAFT)
                .build();
        applyProposalFields(proposal, payload, 0);
        validateProposal(proposal, true);
        return checkScheduleConflicts(proposal, excludeProposalId);
    }

    @Override
    @Transactional(readOnly = true)
    public ClassroomProposalAvailabilityResponse getAvailability(
            CreateClassroomProposalRequest payload,
            Long excludeProposalId,
            String staffEmail
    ) {
        validateProposalPayload(payload);
        requireStaff(staffEmail);
        TrainingProgram courseOffering = requirePublishedOffering(payload.getCourseOfferingId());
        ClassroomProposal proposal = ClassroomProposal.builder()
                .courseOffering(courseOffering)
                .deliveryType(courseOffering.getDeliveryMode())
                .approvalStatus(ClassroomApprovalStatus.DRAFT)
                .build();
        applyProposalFields(proposal, payload, 0);
        validateProposal(proposal, false);

        proposal.setRoom(null);
        List<ClassroomPickerOptionResponse> availableTeachers = userRepository
                .findDistinctByRoles_CodeIn(Set.of(RoleEnum.TEACHER))
                .stream()
                .sorted(Comparator.comparing(User::getFullName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .filter(teacher -> {
                    proposal.setPrimaryTeacher(teacher);
                    return checkScheduleConflicts(proposal, excludeProposalId).getConflicts().stream()
                            .noneMatch(conflict -> conflict.getType() == ConflictType.TEACHER_SCHEDULE
                                    || conflict.getType() == ConflictType.LARK_TEACHER_OVERLAP);
                })
                .map(teacher -> ClassroomPickerOptionResponse.builder()
                        .id(teacher.getId())
                        .label((StringUtils.hasText(teacher.getFullName()) ? teacher.getFullName() : teacher.getEmail())
                                + " - " + teacher.getEmail())
                        .build())
                .toList();

        proposal.setPrimaryTeacher(null);
        List<ClassroomPickerOptionResponse> availableRooms = proposal.getDeliveryType() == ClassroomDeliveryMode.VIRTUAL
                ? List.of()
                : roomRepository.findByActiveTrue().stream()
                .filter(room -> room.getCapacity() == null || room.getCapacity() >= proposal.getCapacity())
                .sorted(Comparator.comparing(ClassroomRoom::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .filter(room -> {
                    proposal.setRoom(room);
                    return checkScheduleConflicts(proposal, excludeProposalId).getConflicts().stream()
                            .noneMatch(conflict -> conflict.getType() == ConflictType.ROOM_SCHEDULE);
                })
                .map(room -> ClassroomPickerOptionResponse.builder()
                        .id(room.getId())
                        .label(room.getName())
                        .capacity(room.getCapacity())
                        .build())
                .toList();

        return ClassroomProposalAvailabilityResponse.builder()
                .teachers(availableTeachers)
                .rooms(availableRooms)
                .build();
    }

    @Override
    public ClassroomProposalResponse submit(Long proposalId, String staffEmail) {
        User staff = requireStaff(staffEmail);
        ClassroomProposal proposal = requireProposal(proposalId);
        if (proposal.getApprovalStatus() != ClassroomApprovalStatus.DRAFT
                && proposal.getApprovalStatus() != ClassroomApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("Chỉ có thể gửi duyệt đề xuất nháp hoặc bị từ chối.");
        }
        validateProposal(proposal, true);
        scheduleLockService.lockDates(sessionDates(proposal));
        assertNoScheduleConflicts(proposal, proposal.getId());
        proposal.setApprovalStatus(ClassroomApprovalStatus.PENDING_APPROVAL);
        proposal.setSubmittedBy(staff);
        proposal.setSubmittedAt(LocalDateTime.now());
        proposal.setReviewedBy(null);
        proposal.setReviewedAt(null);
        proposal.setReviewNote(null);
        return toResponse(proposalRepository.save(proposal));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomProposalResponse> listForStaff(
            ClassroomApprovalStatus status,
            String staffEmail
    ) {
        requireStaff(staffEmail);
        return list(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClassroomProposalResponse> listForManager(
            ClassroomApprovalStatus status,
            String managerEmail
    ) {
        requireApprover(managerEmail);
        ClassroomApprovalStatus resolved = status == null ? ClassroomApprovalStatus.PENDING_APPROVAL : status;
        return list(resolved);
    }

    @Override
    public ClassroomProposalResponse approve(Long proposalId, String managerEmail) {
        User manager = requireApprover(managerEmail);
        ClassroomProposal proposal = requireProposal(proposalId);
        if (proposal.getApprovalStatus() != ClassroomApprovalStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Đề xuất không ở trạng thái chờ duyệt.");
        }
        validateProposal(proposal, true);
        scheduleLockService.lockDates(sessionDates(proposal));
        assertNoScheduleConflicts(proposal, proposal.getId());

        List<LocalDate> dates = sessionDates(proposal);
        List<CurriculumSessionPlan> sessionPlans = proposal.getCourseOffering().getCurriculumProgram() == null
                ? List.of()
                : curriculumSessionPlanRepository.findByProgramIdOrderBySessionNumberAsc(
                        proposal.getCourseOffering().getCurriculumProgram().getId()
                );
        if (!sessionPlans.isEmpty() && dates.size() != sessionPlans.size()) {
            throw new IllegalArgumentException(
                    "Lịch lớp hiện tạo ra " + dates.size() + " buổi nhưng giáo trình yêu cầu "
                            + sessionPlans.size()
                            + " buổi. Vui lòng điều chỉnh ngày bắt đầu, ngày kết thúc hoặc lịch học trước khi duyệt."
            );
        }

        ClassroomOfferingResponse created = classroomOfferingService.createOffering(
                toOfferingRequest(proposal),
                managerEmail
        );
        for (int index = 0; index < dates.size(); index++) {
            LocalDate sessionDate = dates.get(index);
            CurriculumSessionPlan sessionPlan = sessionPlans.isEmpty() ? null : sessionPlans.get(index);
            classroomOfferingService.createSession(created.getId(), CreateClassroomSessionRequest.builder()
                    .sessionDate(sessionDate)
                    .startTime(proposal.getSessionStartTime())
                    .endTime(proposal.getSessionEndTime())
                    .teacherId(proposal.getPrimaryTeacher() == null ? null : proposal.getPrimaryTeacher().getId())
                    .status(ClassroomSessionStatus.SCHEDULED)
                    .deliveryMode(proposal.getDeliveryType())
                    .roomId(proposal.getRoom() == null ? null : proposal.getRoom().getId())
                    .larkMeetingUrl(null)
                    .curriculumSessionPlanId(sessionPlan == null ? null : sessionPlan.getId())
                    .sessionContent(sessionPlan == null
                            ? proposal.getCourseOffering().getTitle()
                            : sessionPlan.getTitle())
                    .note("Sinh từ đề xuất " + proposal.getProposalCode())
                    .build());
        }

        ClassroomOffering classroom = offeringRepository.findById(created.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lớp vừa được tạo."));
        proposal.setApprovedClassroom(classroom);
        proposal.setApprovalStatus(ClassroomApprovalStatus.APPROVED);
        proposal.setReviewedBy(manager);
        proposal.setReviewedAt(LocalDateTime.now());
        proposal.setReviewNote("Đã duyệt mở lớp.");
        return toResponse(proposalRepository.save(proposal));
    }

    @Override
    public ClassroomProposalResponse reject(
            Long proposalId,
            RejectClassroomProposalRequest payload,
            String managerEmail
    ) {
        User manager = requireApprover(managerEmail);
        ClassroomProposal proposal = requireProposal(proposalId);
        if (proposal.getApprovalStatus() != ClassroomApprovalStatus.PENDING_APPROVAL) {
            throw new IllegalArgumentException("Đề xuất không ở trạng thái chờ duyệt.");
        }
        proposal.setApprovalStatus(ClassroomApprovalStatus.REJECTED);
        proposal.setReviewedBy(manager);
        proposal.setReviewedAt(LocalDateTime.now());
        proposal.setReviewNote(payload.getReason().trim());
        return toResponse(proposalRepository.save(proposal));
    }

    private List<ClassroomProposalResponse> list(ClassroomApprovalStatus status) {
        List<ClassroomProposal> proposals = status == null
                ? proposalRepository.findAllByOrderByCreatedAtDesc()
                : proposalRepository.findByApprovalStatusOrderByCreatedAtAsc(status);
        return proposals.stream().map(this::toResponse).toList();
    }

    private void applyProposalFields(
            ClassroomProposal proposal,
            CreateClassroomProposalRequest payload,
            int learnerCount
    ) {
        proposal.setTitle(payload.getTitle().trim());
        proposal.setCapacity(payload.getCapacity() == null
                ? proposal.getCourseOffering().getMaxCapacity()
                : payload.getCapacity());
        proposal.setPlannedStartDate(payload.getPlannedStartDate());
        proposal.setPlannedEndDate(payload.getPlannedEndDate());
        proposal.setScheduleWeekdays(payload.getWeekdays().stream()
                .distinct()
                .map(DayOfWeek::name)
                .collect(Collectors.joining(",")));
        proposal.setSessionStartTime(payload.getSessionStartTime());
        proposal.setSessionEndTime(payload.getSessionEndTime());
        proposal.setPrimaryTeacher(resolveTeacher(payload.getPrimaryTeacherId()));
        boolean offline = proposal.getDeliveryType() == ClassroomDeliveryMode.OFFLINE;
        proposal.setRoom(offline ? resolveRoom(payload.getRoomId()) : null);
        proposal.setOfflineAddress(null);
        proposal.setVirtualMeetingUrl(null);
        proposal.setStaffNote(trimOrNull(payload.getNote()));
        validateProposal(proposal, false);
        if (proposal.getCapacity() < learnerCount) {
            throw new IllegalArgumentException("Sức chứa đề xuất nhỏ hơn số học viên đã chọn.");
        }
    }

    private void validateProposalPayload(CreateClassroomProposalRequest payload) {
        if (payload == null) {
            throw new IllegalArgumentException("Dữ liệu đề xuất lớp không được để trống.");
        }
        if (!StringUtils.hasText(payload.getTitle())) {
            throw new IllegalArgumentException("Tên đề xuất lớp không được để trống.");
        }
        if (payload.getCourseOfferingId() == null) {
            throw new IllegalArgumentException("Khóa học không được để trống.");
        }
        if (payload.getCapacity() == null || payload.getCapacity() < 1) {
            throw new IllegalArgumentException("Sức chứa phải lớn hơn 0.");
        }
        if (payload.getPlannedStartDate() == null || payload.getPlannedEndDate() == null) {
            throw new IllegalArgumentException("Ngày bắt đầu và ngày kết thúc không được để trống.");
        }
        if (!payload.isDateRangeValid()) {
            throw new IllegalArgumentException("Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        }
        if (payload.getWeekdays() == null || payload.getWeekdays().isEmpty()) {
            throw new IllegalArgumentException("Cần chọn ít nhất một ngày học trong tuần.");
        }
        if (payload.getSessionStartTime() == null || payload.getSessionEndTime() == null) {
            throw new IllegalArgumentException("Giờ bắt đầu và giờ kết thúc không được để trống.");
        }
        if (!payload.isTimeRangeValid()) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu.");
        }
    }

    private void validateProposal(ClassroomProposal proposal, boolean requireResources) {
        if (proposal.getCapacity() == null || proposal.getCapacity() < 1) {
            throw new IllegalArgumentException("Sức chứa phải lớn hơn 0.");
        }
        if (proposal.getPlannedStartDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException("Ngày bắt đầu lớp không được ở trong quá khứ.");
        }
        if (proposal.getPlannedEndDate().isBefore(proposal.getPlannedStartDate())) {
            throw new IllegalArgumentException("Ngày kết thúc phải từ ngày bắt đầu trở đi.");
        }
        if (proposal.getPlannedEndDate().isAfter(proposal.getPlannedStartDate().plusYears(1))) {
            throw new IllegalArgumentException("Khoảng thời gian đề xuất không được vượt quá một năm.");
        }
        if (!proposal.getSessionEndTime().isAfter(proposal.getSessionStartTime())) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu.");
        }
        if (proposal.getMembers().size() > proposal.getCapacity()) {
            throw new IllegalArgumentException("Số học viên vượt quá sức chứa đề xuất.");
        }
        if (sessionDates(proposal).isEmpty()) {
            throw new IllegalArgumentException("Khoảng ngày không chứa ngày học đã chọn.");
        }
        if (proposal.getRoom() != null
                && proposal.getRoom().getCapacity() != null
                && proposal.getRoom().getCapacity() < proposal.getCapacity()) {
            throw new IllegalArgumentException("Sức chứa phòng nhỏ hơn sức chứa đề xuất.");
        }
        if (!requireResources) return;
        if (proposal.getPrimaryTeacher() == null) {
            throw new IllegalArgumentException("Cần chọn giáo viên dự kiến trước khi gửi duyệt.");
        }
        if (proposal.getDeliveryType() == ClassroomDeliveryMode.OFFLINE) {
            if (proposal.getRoom() == null) {
                throw new IllegalArgumentException("Lớp Offline cần chọn phòng học.");
            }
        }
    }

    private void assertNoScheduleConflicts(ClassroomProposal proposal, Long excludeProposalId) {
        ConflictCheckResultResponse result = checkScheduleConflicts(proposal, excludeProposalId);
        if (result.isHasBlockingConflict()) {
            throw new ClassroomConflictException("Lịch dự kiến đang trùng với lớp hoặc đề xuất khác.", result);
        }
    }

    private ConflictCheckResultResponse checkScheduleConflicts(
            ClassroomProposal proposal,
            Long excludeProposalId
    ) {
        List<ConflictItemResponse> conflicts = new ArrayList<>();
        for (LocalDate date : sessionDates(proposal)) {
            ConflictCheckResultResponse sessionResult = conflictService.check(ConflictCheckRequest.builder()
                    .teacherId(proposal.getPrimaryTeacher() == null ? null : proposal.getPrimaryTeacher().getId())
                    .roomId(proposal.getRoom() == null ? null : proposal.getRoom().getId())
                    .sessionDate(date)
                    .startTime(proposal.getSessionStartTime())
                    .endTime(proposal.getSessionEndTime())
                    .learnerIds(List.of())
                    .checkCapacity(false)
                    .larkMeetingUrl(proposal.getVirtualMeetingUrl())
                    .build());
            if (sessionResult != null && sessionResult.getConflicts() != null) {
                sessionResult.getConflicts().stream()
                        .map(item -> withConflictDate(item, date))
                        .forEach(conflicts::add);
            }
        }
        conflicts.addAll(findPendingProposalConflicts(proposal, excludeProposalId));
        return ConflictCheckResultResponse.builder()
                .hasBlockingConflict(!conflicts.isEmpty())
                .canOverride(false)
                .conflicts(conflicts)
                .build();
    }

    private ConflictItemResponse withConflictDate(ConflictItemResponse item, LocalDate date) {
        String message = StringUtils.hasText(item.getMessage())
                ? item.getMessage()
                : "Phát hiện xung đột lịch.";
        return ConflictItemResponse.builder()
                .type(item.getType())
                .message(message + " Ngày " + date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".")
                .details(item.getDetails())
                .build();
    }

    private List<ConflictItemResponse> findPendingProposalConflicts(
            ClassroomProposal proposal,
            Long excludeProposalId
    ) {
        List<ClassroomProposal> pending = proposalRepository.findByApprovalStatusOrderByCreatedAtAsc(
                ClassroomApprovalStatus.PENDING_APPROVAL
        );
        if (pending == null || pending.isEmpty()) return List.of();

        List<ConflictItemResponse> conflicts = new ArrayList<>();
        for (ClassroomProposal other : pending) {
            if (other.getId() != null && other.getId().equals(excludeProposalId)) continue;
            if (!timeRangesOverlap(proposal, other)) continue;
            LocalDate overlapDate = firstSharedSessionDate(proposal, other);
            if (overlapDate == null) continue;

            if (proposal.getPrimaryTeacher() != null
                    && other.getPrimaryTeacher() != null
                    && proposal.getPrimaryTeacher().getId().equals(other.getPrimaryTeacher().getId())) {
                conflicts.add(ConflictItemResponse.builder()
                        .type(ConflictType.TEACHER_SCHEDULE)
                        .message("Giáo viên đã được xếp trong đề xuất "
                                + other.getProposalCode() + " vào ngày " + overlapDate + ".")
                        .details(Map.of(
                                "proposalId", other.getId(),
                                "proposalCode", other.getProposalCode(),
                                "overlapDate", overlapDate.toString(),
                                "overlapStart", other.getSessionStartTime().toString(),
                                "overlapEnd", other.getSessionEndTime().toString()
                        ))
                        .build());
            }
            if (proposal.getRoom() != null
                    && other.getRoom() != null
                    && proposal.getRoom().getId().equals(other.getRoom().getId())) {
                conflicts.add(ConflictItemResponse.builder()
                        .type(ConflictType.ROOM_SCHEDULE)
                        .message("Phòng học đã được giữ trong đề xuất "
                                + other.getProposalCode() + " vào ngày " + overlapDate + ".")
                        .details(Map.of(
                                "proposalId", other.getId(),
                                "proposalCode", other.getProposalCode(),
                                "overlapDate", overlapDate.toString(),
                                "overlapStart", other.getSessionStartTime().toString(),
                                "overlapEnd", other.getSessionEndTime().toString()
                        ))
                        .build());
            }
        }
        return conflicts;
    }

    private boolean timeRangesOverlap(ClassroomProposal left, ClassroomProposal right) {
        return left.getSessionStartTime().isBefore(right.getSessionEndTime())
                && left.getSessionEndTime().isAfter(right.getSessionStartTime());
    }

    private LocalDate firstSharedSessionDate(ClassroomProposal left, ClassroomProposal right) {
        Set<LocalDate> rightDates = Set.copyOf(sessionDates(right));
        return sessionDates(left).stream()
                .filter(rightDates::contains)
                .findFirst()
                .orElse(null);
    }

    private List<LocalDate> sessionDates(ClassroomProposal proposal) {
        Set<DayOfWeek> weekdays = parseWeekdays(proposal.getScheduleWeekdays());
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate date = proposal.getPlannedStartDate();
             !date.isAfter(proposal.getPlannedEndDate());
             date = date.plusDays(1)) {
            if (weekdays.contains(date.getDayOfWeek())) dates.add(date);
        }
        return dates;
    }

    private Set<DayOfWeek> parseWeekdays(String value) {
        if (!StringUtils.hasText(value)) return EnumSet.noneOf(DayOfWeek.class);
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }

    private CreateClassroomOfferingRequest toOfferingRequest(ClassroomProposal proposal) {
        TrainingProgram source = proposal.getCourseOffering();
        return CreateClassroomOfferingRequest.builder()
                .title(proposal.getTitle())
                .shortDescription(source.getShortDescription())
                .description(source.getDescription())
                .deliveryMode(proposal.getDeliveryType())
                .classroomStatus(ClassroomOfferingStatus.UPCOMING)
                .packageStatus(PackageStatus.PUBLISHED)
                .trainingProgramId(source.getId())
                .entryLevel(null)
                .maxCapacity(proposal.getCapacity())
                .startDate(proposal.getPlannedStartDate())
                .endDate(proposal.getPlannedEndDate())
                .primaryTeacherId(proposal.getPrimaryTeacher().getId())
                .defaultRoomId(proposal.getRoom() == null ? null : proposal.getRoom().getId())
                .offlineAddress(proposal.getOfflineAddress())
                .locationNote(scheduleLabel(proposal))
                .defaultLarkMeetingUrl(null)
                .studyMode(scheduleLabel(proposal))
                .build();
    }

    private ClassroomProposalResponse toResponse(ClassroomProposal proposal) {
        List<ClassroomProposalMemberResponse> members = proposal.getMembers().stream()
                .map(this::toMemberResponse)
                .toList();
        Map<PlacementLevel, Long> distribution = members.stream()
                .collect(Collectors.groupingBy(
                        ClassroomProposalMemberResponse::getPlacementLevel,
                        Collectors.counting()
                ));
        return ClassroomProposalResponse.builder()
                .id(proposal.getId())
                .proposalCode(proposal.getProposalCode())
                .title(proposal.getTitle())
                .courseOfferingId(proposal.getCourseOffering().getId())
                .courseOfferingTitle(proposal.getCourseOffering().getTitle())
                .deliveryType(proposal.getDeliveryType())
                .placementLevel(proposal.getPlacementLevel())
                .capacity(proposal.getCapacity())
                .learnerCount(members.size())
                .levelDistribution(distribution)
                .plannedStartDate(proposal.getPlannedStartDate())
                .plannedEndDate(proposal.getPlannedEndDate())
                .weekdays(new ArrayList<>(parseWeekdays(proposal.getScheduleWeekdays())))
                .sessionStartTime(proposal.getSessionStartTime())
                .sessionEndTime(proposal.getSessionEndTime())
                .plannedSessionCount(sessionDates(proposal).size())
                .primaryTeacherId(proposal.getPrimaryTeacher() == null ? null : proposal.getPrimaryTeacher().getId())
                .primaryTeacherName(proposal.getPrimaryTeacher() == null ? null : proposal.getPrimaryTeacher().getFullName())
                .roomId(proposal.getRoom() == null ? null : proposal.getRoom().getId())
                .roomName(proposal.getRoom() == null ? null : proposal.getRoom().getName())
                .offlineAddress(proposal.getOfflineAddress())
                .virtualMeetingUrl(proposal.getVirtualMeetingUrl())
                .staffNote(proposal.getStaffNote())
                .approvalStatus(proposal.getApprovalStatus())
                .approvalStatusLabel(statusLabel(proposal.getApprovalStatus()))
                .createdById(proposal.getCreatedBy().getId())
                .createdByName(proposal.getCreatedBy().getFullName())
                .submittedAt(proposal.getSubmittedAt())
                .reviewedById(proposal.getReviewedBy() == null ? null : proposal.getReviewedBy().getId())
                .reviewedByName(proposal.getReviewedBy() == null ? null : proposal.getReviewedBy().getFullName())
                .reviewedAt(proposal.getReviewedAt())
                .reviewNote(proposal.getReviewNote())
                .approvedClassroomId(proposal.getApprovedClassroom() == null ? null : proposal.getApprovedClassroom().getId())
                .members(members)
                .createdAt(proposal.getCreatedAt())
                .updatedAt(proposal.getUpdatedAt())
                .build();
    }

    private ClassroomProposalMemberResponse toMemberResponse(ClassroomProposalMember member) {
        EnrollmentRequest request = member.getEnrollmentRequest();
        return ClassroomProposalMemberResponse.builder()
                .enrollmentRequestId(request.getId())
                .learnerId(request.getLearner().getId())
                .learnerName(request.getLearner().getFullName())
                .learnerEmail(request.getLearner().getEmail())
                .placementLevel(request.getConfirmedLevel())
                .preferredSchedule(request.getPreferredSchedule())
                .campusPreference(request.getCampusPreference())
                .classroomEnrollmentId(member.getClassroomEnrollmentId())
                .build();
    }

    private User requireStaff(String email) {
        User user = requireUser(email);
        if (!TrainingRolePolicy.canPerformStaffAction(user)) {
            throw new IllegalArgumentException("Bạn không có quyền tạo hoặc gửi đề xuất lớp.");
        }
        return user;
    }

    private User requireApprover(String email) {
        User user = requireUser(email);
        if (!TrainingRolePolicy.canApprove(user)) {
            throw new IllegalArgumentException("Chỉ Manager mới có quyền duyệt đề xuất lớp.");
        }
        return user;
    }

    private User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng."));
    }

    private TrainingProgram requirePublishedOffering(Long id) {
        TrainingProgram offering = trainingProgramRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học."));
        if (offering.getStatus() != PackageStatus.PUBLISHED) {
            throw new IllegalArgumentException("Chỉ có thể đề xuất lớp từ khóa học đã xuất bản.");
        }
        return offering;
    }

    private ClassroomProposal requireProposal(Long id) {
        return proposalRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đề xuất lớp."));
    }

    private User resolveTeacher(Long id) {
        if (id == null) return null;
        User teacher = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giáo viên."));
        if (!teacher.hasRole(RoleEnum.TEACHER)) {
            throw new IllegalArgumentException("Người được chọn không có vai trò Giáo viên.");
        }
        return teacher;
    }

    private ClassroomRoom resolveRoom(Long id) {
        if (id == null) return null;
        ClassroomRoom room = roomRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng học."));
        if (!room.isActive()) {
            throw new IllegalArgumentException("Phòng học đã ngừng hoạt động.");
        }
        return room;
    }

    private String scheduleLabel(ClassroomProposal proposal) {
        return proposal.getScheduleWeekdays().replace(',', ' ')
                + " · " + proposal.getSessionStartTime()
                + "–" + proposal.getSessionEndTime();
    }

    private String trimOrNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String statusLabel(ClassroomApprovalStatus status) {
        return switch (status) {
            case DRAFT -> "Bản nháp";
            case PENDING_APPROVAL -> "Chờ Manager duyệt";
            case APPROVED -> "Đã duyệt";
            case REJECTED -> "Đã từ chối";
            case CANCELLED -> "Đã hủy";
        };
    }
}
