package fu.sep490.g23.backend.service.classroom;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sep490.g23.backend.entity.classroom.ClassroomProposalMember;

import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomOfferingRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomProposalRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomSessionRequest;
import fu.sep490.g23.backend.dto.request.classroom.RejectClassroomProposalRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomProposalResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictItemResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassroomProposal;
import fu.sep490.g23.backend.entity.classroom.Room;
import fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest;
import fu.sep490.g23.backend.entity.classroom.TrainingProgram;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomApprovalStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ConflictType;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sep490.g23.backend.entity.curriculum.CurriculumSessionPlan;
import fu.sep490.g23.backend.entity.curriculum.CurriculumUnit;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.exception.ClassroomConflictException;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomProposalMemberRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomProposalRepository;
import fu.sep490.g23.backend.repository.classroom.RoomRepository;
import fu.sep490.g23.backend.repository.classroom.CourseRegistrationRequestRepository;
import fu.sep490.g23.backend.repository.classroom.EnrollmentRequestStatusHistoryRepository;
import fu.sep490.g23.backend.repository.classroom.TrainingProgramRepository;
import fu.sep490.g23.backend.repository.curriculum.CurriculumSessionPlanRepository;
import fu.sep490.g23.backend.service.classroom.impl.ClassroomProposalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomProposalServiceImplTest {

    @Mock private ClassroomProposalRepository proposalRepository;
    @Mock private ClassroomProposalMemberRepository memberRepository;
    @Mock private CourseRegistrationRequestRepository enrollmentRequestRepository;
    @Mock private EnrollmentRequestStatusHistoryRepository enrollmentHistoryRepository;
    @Mock private TrainingProgramRepository trainingProgramRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private ClassSectionRepository offeringRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClassroomConflictService conflictService;
    @Mock private ClassroomScheduleLockService scheduleLockService;
    @Mock private ClassroomOfferingService classroomOfferingService;
    @Mock private CurriculumSessionPlanRepository curriculumSessionPlanRepository;

    private ClassroomProposalServiceImpl service;
    private User learner;
    private User staff;
    private User manager;
    private User teacher;
    private TrainingProgram courseOffering;
    private Room room;
    private CourseRegistrationRequest courseRegistrationRequest;

    @BeforeEach
    void setUp() {
        service = new ClassroomProposalServiceImpl(
                proposalRepository,
                trainingProgramRepository,
                roomRepository,
                offeringRepository,
                userRepository,
                conflictService,
                scheduleLockService,
                classroomOfferingService,
                curriculumSessionPlanRepository
        );
        learner = user(1L, "learner@example.com", RoleEnum.LEARNER);
        staff = user(2L, "staff@example.com", RoleEnum.STAFF);
        manager = user(3L, "manager@example.com", RoleEnum.MANAGER);
        teacher = user(4L, "teacher@example.com", RoleEnum.TEACHER);
        courseOffering = TrainingProgram.builder()
                .id(10L)
                .title("IELTS Foundation Offline")
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .status(PackageStatus.PUBLISHED)
                .build();
        room = Room.builder().id(11L).name("A101").capacity(25).active(true).build();
        courseRegistrationRequest = CourseRegistrationRequest.builder()
                .id(12L)
                .learner(learner)
                .courseOffering(courseOffering)
                .confirmedLevel(PlacementLevel.BEGINNER)
                .status(EnrollmentRequestStatus.WAITING_FOR_CLASS)
                .build();
    }

    @Test
    void createDraftDoesNotRequireOrMoveLearners() {
        CreateClassroomProposalRequest payload = proposalPayload();
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(trainingProgramRepository.findById(courseOffering.getId())).thenReturn(Optional.of(courseOffering));
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(proposalRepository.save(any(ClassroomProposal.class))).thenAnswer(invocation -> {
            ClassroomProposal proposal = invocation.getArgument(0);
            proposal.setId(50L);
            return proposal;
        });

        ClassroomProposalResponse response = service.create(payload, staff.getEmail());

        assertThat(response.getApprovalStatus()).isEqualTo(ClassroomApprovalStatus.DRAFT);
        assertThat(response.getLearnerCount()).isZero();
        assertThat(response.getDeliveryType()).isEqualTo(ClassroomDeliveryMode.OFFLINE);
        assertThat(courseRegistrationRequest.getStatus()).isEqualTo(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        verify(scheduleLockService).lockDates(any());
        verify(enrollmentHistoryRepository, never()).save(any());
    }

    @Test
    void createDraftUsesCourseDeliveryModeInsteadOfClientValue() {
        courseOffering.setDeliveryMode(ClassroomDeliveryMode.VIRTUAL);
        CreateClassroomProposalRequest payload = proposalPayload();
        payload.setDeliveryType(ClassroomDeliveryMode.OFFLINE);
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(trainingProgramRepository.findById(courseOffering.getId())).thenReturn(Optional.of(courseOffering));
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(proposalRepository.save(any(ClassroomProposal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ClassroomProposalResponse response = service.create(payload, staff.getEmail());

        assertThat(response.getDeliveryType()).isEqualTo(ClassroomDeliveryMode.VIRTUAL);
        verify(roomRepository, never()).findById(any());
    }

    @Test
    void createDraftRejectsSelectedTeacherScheduleConflict() {
        CreateClassroomProposalRequest payload = proposalPayload();
        ConflictCheckResultResponse conflict = ConflictCheckResultResponse.builder()
                .hasBlockingConflict(true)
                .conflicts(List.of(ConflictItemResponse.builder()
                        .type(ConflictType.TEACHER_SCHEDULE)
                        .message("Giáo viên đã có lịch dạy khác trong khung giờ này.")
                        .build()))
                .build();
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(trainingProgramRepository.findById(courseOffering.getId())).thenReturn(Optional.of(courseOffering));
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(conflictService.check(any())).thenReturn(conflict);

        assertThatThrownBy(() -> service.create(payload, staff.getEmail()))
                .isInstanceOf(ClassroomConflictException.class)
                .hasMessageContaining("Lịch dự kiến đang trùng");

        verify(proposalRepository, never()).save(any());
    }

    @Test
    void validateScheduleReportsConflictWithPendingProposal() {
        CreateClassroomProposalRequest payload = proposalPayload();
        ClassroomProposal pending = pendingProposal();
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(trainingProgramRepository.findById(courseOffering.getId())).thenReturn(Optional.of(courseOffering));
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));
        when(proposalRepository.findByApprovalStatusOrderByCreatedAtAsc(
                ClassroomApprovalStatus.PENDING_APPROVAL
        )).thenReturn(List.of(pending));

        ConflictCheckResultResponse result = service.validateSchedule(payload, null, staff.getEmail());

        assertThat(result.isHasBlockingConflict()).isTrue();
        assertThat(result.getConflicts())
                .extracting(ConflictItemResponse::getType)
                .contains(ConflictType.TEACHER_SCHEDULE, ConflictType.ROOM_SCHEDULE);
    }

    @Test
    void createDraftRejectsRoomSmallerThanPlannedCapacity() {
        CreateClassroomProposalRequest payload = proposalPayload();
        room.setCapacity(10);
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(trainingProgramRepository.findById(courseOffering.getId())).thenReturn(Optional.of(courseOffering));
        when(userRepository.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(roomRepository.findById(room.getId())).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.create(payload, staff.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Sức chứa phòng nhỏ hơn sức chứa đề xuất.");

        verify(proposalRepository, never()).save(any());
    }

    @Test
    void managerApprovalCreatesOfficialClassAndSessionsWithoutAutoEnrollment() {
        ClassroomProposal proposal = pendingProposal();
        ClassSection classroom = ClassSection.builder().id(100L).build();
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        when(proposalRepository.findById(proposal.getId())).thenReturn(Optional.of(proposal));
        when(classroomOfferingService.createOffering(any(CreateClassroomOfferingRequest.class), any()))
                .thenReturn(ClassroomOfferingResponse.builder().id(classroom.getId()).build());
        when(offeringRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(proposalRepository.save(proposal)).thenReturn(proposal);

        ClassroomProposalResponse response = service.approve(proposal.getId(), manager.getEmail());

        assertThat(response.getApprovalStatus()).isEqualTo(ClassroomApprovalStatus.APPROVED);
        assertThat(response.getApprovedClassroomId()).isEqualTo(classroom.getId());
        assertThat(courseRegistrationRequest.getStatus()).isEqualTo(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        verify(scheduleLockService).lockDates(any());
        ArgumentCaptor<CreateClassroomSessionRequest> sessionCaptor =
                ArgumentCaptor.forClass(CreateClassroomSessionRequest.class);
        verify(classroomOfferingService, times(2)).createSession(any(), sessionCaptor.capture());
        assertThat(sessionCaptor.getAllValues()).allSatisfy(request -> {
            assertThat(request.getCurriculumSessionPlanId()).isNull();
            assertThat(request.getSessionContent()).isEqualTo(courseOffering.getTitle());
        });
        verify(classroomOfferingService, never()).enrollStudent(any(), any());
    }

    @Test
    void managerApprovalCreatesVirtualRoomsAutomaticallyWithoutManualLink() {
        courseOffering.setDeliveryMode(ClassroomDeliveryMode.VIRTUAL);
        ClassroomProposal proposal = pendingProposal();
        proposal.setDeliveryType(ClassroomDeliveryMode.VIRTUAL);
        proposal.setRoom(null);
        proposal.setOfflineAddress(null);
        proposal.setVirtualMeetingUrl(null);
        ClassSection classroom = ClassSection.builder().id(101L).build();
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        when(proposalRepository.findById(proposal.getId())).thenReturn(Optional.of(proposal));
        when(classroomOfferingService.createOffering(any(CreateClassroomOfferingRequest.class), any()))
                .thenReturn(ClassroomOfferingResponse.builder().id(classroom.getId()).build());
        when(offeringRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(proposalRepository.save(proposal)).thenReturn(proposal);

        service.approve(proposal.getId(), manager.getEmail());

        ArgumentCaptor<CreateClassroomOfferingRequest> offeringCaptor =
                ArgumentCaptor.forClass(CreateClassroomOfferingRequest.class);
        verify(classroomOfferingService).createOffering(offeringCaptor.capture(), any());
        assertThat(offeringCaptor.getValue().getDefaultLarkMeetingUrl()).isNull();

        ArgumentCaptor<CreateClassroomSessionRequest> sessionCaptor =
                ArgumentCaptor.forClass(CreateClassroomSessionRequest.class);
        verify(classroomOfferingService, times(2)).createSession(any(), sessionCaptor.capture());
        assertThat(sessionCaptor.getAllValues())
                .allSatisfy(request -> {
                    assertThat(request.getDeliveryMode()).isEqualTo(ClassroomDeliveryMode.VIRTUAL);
                    assertThat(request.getLarkMeetingUrl()).isNull();
                });
    }

    @Test
    void managerApprovalMapsFiveDatesToFiveStructuredSessionPlans() {
        ClassroomProposal proposal = structuredPendingProposal(5);
        ClassSection classroom = ClassSection.builder().id(102L).build();
        List<CurriculumSessionPlan> plans = structuredPlans(proposal.getCourseOffering().getCurriculumProgram(), 5);
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        when(proposalRepository.findById(proposal.getId())).thenReturn(Optional.of(proposal));
        when(curriculumSessionPlanRepository.findByProgramIdOrderBySessionNumberAsc(77L)).thenReturn(plans);
        when(classroomOfferingService.createOffering(any(CreateClassroomOfferingRequest.class), any()))
                .thenReturn(ClassroomOfferingResponse.builder().id(classroom.getId()).build());
        when(offeringRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(proposalRepository.save(proposal)).thenReturn(proposal);

        service.approve(proposal.getId(), manager.getEmail());

        ArgumentCaptor<CreateClassroomSessionRequest> captor = ArgumentCaptor.forClass(CreateClassroomSessionRequest.class);
        verify(classroomOfferingService, times(5)).createSession(any(), captor.capture());
        assertThat(captor.getAllValues()).hasSize(5);
        assertThat(captor.getAllValues().get(0).getCurriculumSessionPlanId()).isEqualTo(1L);
        assertThat(captor.getAllValues().get(0).getSessionContent()).isEqualTo("Nội dung buổi 1");
        assertThat(captor.getAllValues().get(4).getCurriculumSessionPlanId()).isEqualTo(5L);
        assertThat(captor.getAllValues().get(4).getSessionContent()).isEqualTo("Nội dung buổi 5");
    }

    @Test
    void managerApprovalRejectsFourDatesForFiveStructuredPlans() {
        ClassroomProposal proposal = structuredPendingProposal(4);
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        when(proposalRepository.findById(proposal.getId())).thenReturn(Optional.of(proposal));
        when(curriculumSessionPlanRepository.findByProgramIdOrderBySessionNumberAsc(77L))
                .thenReturn(structuredPlans(proposal.getCourseOffering().getCurriculumProgram(), 5));

        assertThatThrownBy(() -> service.approve(proposal.getId(), manager.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tạo ra 4 buổi nhưng giáo trình yêu cầu 5 buổi");
        verify(classroomOfferingService, never()).createOffering(any(), any());
    }

    @Test
    void managerApprovalRejectsSixDatesForFiveStructuredPlans() {
        ClassroomProposal proposal = structuredPendingProposal(6);
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        when(proposalRepository.findById(proposal.getId())).thenReturn(Optional.of(proposal));
        when(curriculumSessionPlanRepository.findByProgramIdOrderBySessionNumberAsc(77L))
                .thenReturn(structuredPlans(proposal.getCourseOffering().getCurriculumProgram(), 5));

        assertThatThrownBy(() -> service.approve(proposal.getId(), manager.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tạo ra 6 buổi nhưng giáo trình yêu cầu 5 buổi");
        verify(classroomOfferingService, never()).createOffering(any(), any());
    }

    @Test
    void managerRejectionReturnsProposalToStaffWithoutChangingLearners() {
        ClassroomProposal proposal = pendingProposal();
        RejectClassroomProposalRequest payload = new RejectClassroomProposalRequest();
        payload.setReason("Cần đổi phòng học vì sức chứa không phù hợp.");
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        when(proposalRepository.findById(proposal.getId())).thenReturn(Optional.of(proposal));
        when(proposalRepository.save(proposal)).thenReturn(proposal);

        ClassroomProposalResponse response = service.reject(proposal.getId(), payload, manager.getEmail());

        assertThat(response.getApprovalStatus()).isEqualTo(ClassroomApprovalStatus.REJECTED);
        assertThat(response.getReviewNote()).isEqualTo(payload.getReason());
        assertThat(courseRegistrationRequest.getStatus()).isEqualTo(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        verify(enrollmentHistoryRepository, never()).save(any());
    }

    private ClassroomProposal pendingProposal() {
        courseRegistrationRequest.setStatus(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        LocalDate firstMonday = nextMonday();
        ClassroomProposal proposal = ClassroomProposal.builder()
                .id(50L)
                .proposalCode("CP-TEST")
                .title("IELTS Foundation A01")
                .courseOffering(courseOffering)
                .deliveryType(ClassroomDeliveryMode.OFFLINE)
                .placementLevel(null)
                .capacity(20)
                .plannedStartDate(firstMonday)
                .plannedEndDate(firstMonday.plusDays(2))
                .scheduleWeekdays("MONDAY,WEDNESDAY")
                .sessionStartTime(LocalTime.of(18, 30))
                .sessionEndTime(LocalTime.of(20, 30))
                .primaryTeacher(teacher)
                .room(room)
                .offlineAddress("Cơ sở Cầu Giấy")
                .approvalStatus(ClassroomApprovalStatus.PENDING_APPROVAL)
                .createdBy(staff)
                .submittedBy(staff)
                .build();
        return proposal;
    }

    private ClassroomProposal structuredPendingProposal(int dateCount) {
        ClassroomProposal proposal = pendingProposal();
        LocalDate firstMonday = nextMonday();
        proposal.setPlannedStartDate(firstMonday);
        proposal.setPlannedEndDate(firstMonday.plusDays(dateCount - 1L));
        proposal.setScheduleWeekdays("MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY,SATURDAY,SUNDAY");
        courseOffering.setCurriculumProgram(CurriculumProgram.builder().id(77L).build());
        return proposal;
    }

    private List<CurriculumSessionPlan> structuredPlans(CurriculumProgram program, int count) {
        CurriculumUnit unit = CurriculumUnit.builder()
                .id(88L)
                .program(program)
                .title("Reading Fundamentals")
                .build();
        return java.util.stream.IntStream.rangeClosed(1, count)
                .mapToObj(number -> CurriculumSessionPlan.builder()
                        .id((long) number)
                        .unit(unit)
                        .sessionNumber(number)
                        .displayOrder(number)
                        .title("Nội dung buổi " + number)
                        .build())
                .toList();
    }

    private CreateClassroomProposalRequest proposalPayload() {
        LocalDate firstMonday = nextMonday();
        CreateClassroomProposalRequest payload = new CreateClassroomProposalRequest();
        payload.setTitle("IELTS Foundation A01");
        payload.setCourseOfferingId(courseOffering.getId());
        payload.setDeliveryType(ClassroomDeliveryMode.OFFLINE);
        payload.setEnrollmentRequestIds(List.of());
        payload.setCapacity(20);
        payload.setPlannedStartDate(firstMonday);
        payload.setEndDate(firstMonday.plusDays(2));
        payload.setWeekdays(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
        payload.setSessionStartTime(LocalTime.of(18, 30));
        payload.setSessionEndTime(LocalTime.of(20, 30));
        payload.setPrimaryTeacherId(teacher.getId());
        payload.setRoomId(room.getId());
        return payload;
    }

    private LocalDate nextMonday() {
        return LocalDate.now().plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    private User user(Long id, String email, RoleEnum role) {
        User user = User.builder().id(id).fullName(email).email(email).build();
        user.setRole(role);
        return user;
    }
}
