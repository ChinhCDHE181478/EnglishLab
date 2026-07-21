package fu.sap490.g23.backend.service.classroom.impl;

import fu.sap490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateClassroomOfferingRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateClassroomProposalRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateClassroomSessionRequest;
import fu.sap490.g23.backend.dto.request.classroom.RejectClassroomProposalRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomProposalMemberResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomProposalResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.ClassroomProposal;
import fu.sap490.g23.backend.entity.classroom.ClassroomProposalMember;
import fu.sap490.g23.backend.entity.classroom.ClassroomRoom;
import fu.sap490.g23.backend.entity.classroom.EnrollmentRequest;
import fu.sap490.g23.backend.entity.classroom.TrainingProgram;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomApprovalStatus;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomProposalRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomRoomRepository;
import fu.sap490.g23.backend.repository.classroom.TrainingProgramRepository;
import fu.sap490.g23.backend.security.TrainingRolePolicy;
import fu.sap490.g23.backend.service.classroom.ClassroomConflictService;
import fu.sap490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sap490.g23.backend.service.classroom.ClassroomProposalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
    private final ClassroomOfferingService classroomOfferingService;

    @Override
    public ClassroomProposalResponse create(CreateClassroomProposalRequest payload, String staffEmail) {
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
        proposalRepository.save(proposal);
        return toResponse(proposal);
    }

    @Override
    public ClassroomProposalResponse update(
            Long proposalId,
            CreateClassroomProposalRequest payload,
            String staffEmail
    ) {
        User staff = requireStaff(staffEmail);
        ClassroomProposal proposal = requireProposal(proposalId);
        if (proposal.getApprovalStatus() != ClassroomApprovalStatus.DRAFT
                && proposal.getApprovalStatus() != ClassroomApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("Chỉ có thể sửa đề xuất nháp hoặc đã bị từ chối.");
        }
        if (!proposal.getCourseOffering().getId().equals(payload.getCourseOfferingId())) {
            throw new IllegalArgumentException("Không thể đổi khóa học của đề xuất đã tạo.");
        }
        applyProposalFields(proposal, payload, 0);
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
    public ClassroomProposalResponse submit(Long proposalId, String staffEmail) {
        User staff = requireStaff(staffEmail);
        ClassroomProposal proposal = requireProposal(proposalId);
        if (proposal.getApprovalStatus() != ClassroomApprovalStatus.DRAFT
                && proposal.getApprovalStatus() != ClassroomApprovalStatus.REJECTED) {
            throw new IllegalArgumentException("Chỉ có thể gửi duyệt đề xuất nháp hoặc bị từ chối.");
        }
        validateProposal(proposal, true);
        assertNoScheduleConflicts(proposal);
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
        assertNoScheduleConflicts(proposal);

        ClassroomOfferingResponse created = classroomOfferingService.createOffering(
                toOfferingRequest(proposal),
                managerEmail
        );
        for (LocalDate sessionDate : sessionDates(proposal)) {
            classroomOfferingService.createSession(created.getId(), CreateClassroomSessionRequest.builder()
                    .sessionDate(sessionDate)
                    .startTime(proposal.getSessionStartTime())
                    .endTime(proposal.getSessionEndTime())
                    .teacherId(proposal.getPrimaryTeacher() == null ? null : proposal.getPrimaryTeacher().getId())
                    .status(ClassroomSessionStatus.SCHEDULED)
                    .deliveryMode(proposal.getDeliveryType())
                    .roomId(proposal.getRoom() == null ? null : proposal.getRoom().getId())
                    .larkMeetingUrl(proposal.getVirtualMeetingUrl())
                    .sessionContent(proposal.getCourseOffering().getTitle())
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
        proposal.setRoom(resolveRoom(payload.getRoomId()));
        proposal.setOfflineAddress(trimOrNull(payload.getOfflineAddress()));
        proposal.setVirtualMeetingUrl(trimOrNull(payload.getVirtualMeetingUrl()));
        proposal.setStaffNote(trimOrNull(payload.getNote()));
        validateProposal(proposal, false);
        if (proposal.getCapacity() < learnerCount) {
            throw new IllegalArgumentException("Sức chứa đề xuất nhỏ hơn số học viên đã chọn.");
        }
    }

    private void validateProposal(ClassroomProposal proposal, boolean requireResources) {
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
        if (!requireResources) return;
        if (proposal.getPrimaryTeacher() == null) {
            throw new IllegalArgumentException("Cần chọn giáo viên dự kiến trước khi gửi duyệt.");
        }
        if (proposal.getDeliveryType() == ClassroomDeliveryMode.OFFLINE) {
            if (proposal.getRoom() == null) {
                throw new IllegalArgumentException("Khóa Offline cần chọn phòng học.");
            }
            if (proposal.getRoom().getCapacity() != null
                    && proposal.getRoom().getCapacity() < proposal.getCapacity()) {
                throw new IllegalArgumentException("Sức chứa phòng nhỏ hơn sức chứa đề xuất.");
            }
        } else if (!StringUtils.hasText(proposal.getVirtualMeetingUrl())) {
            throw new IllegalArgumentException("Khóa Virtual cần có link phòng học trực tuyến.");
        }
    }

    private void assertNoScheduleConflicts(ClassroomProposal proposal) {
        for (LocalDate date : sessionDates(proposal)) {
            conflictService.assertNoBlockingConflict(ConflictCheckRequest.builder()
                    .teacherId(proposal.getPrimaryTeacher() == null ? null : proposal.getPrimaryTeacher().getId())
                    .roomId(proposal.getRoom() == null ? null : proposal.getRoom().getId())
                    .sessionDate(date)
                    .startTime(proposal.getSessionStartTime())
                    .endTime(proposal.getSessionEndTime())
                    .learnerIds(List.of())
                    .checkCapacity(false)
                    .larkMeetingUrl(proposal.getVirtualMeetingUrl())
                    .build());
        }
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
                .defaultLarkMeetingUrl(proposal.getVirtualMeetingUrl())
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
