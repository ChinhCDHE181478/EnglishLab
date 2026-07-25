package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.CreateClassroomOfferingRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateClassroomProposalRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateClassroomSessionRequest;
import fu.sap490.g23.backend.dto.request.classroom.RejectClassroomProposalRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
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
import fu.sap490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomProposalMemberRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomProposalRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomRoomRepository;
import fu.sap490.g23.backend.repository.classroom.EnrollmentRequestRepository;
import fu.sap490.g23.backend.repository.classroom.EnrollmentRequestStatusHistoryRepository;
import fu.sap490.g23.backend.repository.classroom.TrainingProgramRepository;
import fu.sap490.g23.backend.service.classroom.impl.ClassroomProposalServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomProposalServiceImplTest {

    @Mock private ClassroomProposalRepository proposalRepository;
    @Mock private ClassroomProposalMemberRepository memberRepository;
    @Mock private EnrollmentRequestRepository enrollmentRequestRepository;
    @Mock private EnrollmentRequestStatusHistoryRepository enrollmentHistoryRepository;
    @Mock private TrainingProgramRepository trainingProgramRepository;
    @Mock private ClassroomRoomRepository roomRepository;
    @Mock private ClassroomOfferingRepository offeringRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClassroomConflictService conflictService;
    @Mock private ClassroomOfferingService classroomOfferingService;

    private ClassroomProposalServiceImpl service;
    private User learner;
    private User staff;
    private User manager;
    private User teacher;
    private TrainingProgram courseOffering;
    private ClassroomRoom room;
    private EnrollmentRequest enrollmentRequest;

    @BeforeEach
    void setUp() {
        service = new ClassroomProposalServiceImpl(
                proposalRepository,
                trainingProgramRepository,
                roomRepository,
                offeringRepository,
                userRepository,
                conflictService,
                classroomOfferingService
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
                .maxCapacity(20)
                .build();
        room = ClassroomRoom.builder().id(11L).name("A101").capacity(25).active(true).build();
        enrollmentRequest = EnrollmentRequest.builder()
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
        assertThat(enrollmentRequest.getStatus()).isEqualTo(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        verify(enrollmentHistoryRepository, never()).save(any());
    }

    @Test
    void managerApprovalCreatesOfficialClassAndSessionsWithoutAutoEnrollment() {
        ClassroomProposal proposal = pendingProposal();
        ClassroomOffering classroom = ClassroomOffering.builder().id(100L).build();
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        when(proposalRepository.findById(proposal.getId())).thenReturn(Optional.of(proposal));
        when(classroomOfferingService.createOffering(any(CreateClassroomOfferingRequest.class), any()))
                .thenReturn(ClassroomOfferingResponse.builder().id(classroom.getId()).build());
        when(offeringRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(proposalRepository.save(proposal)).thenReturn(proposal);

        ClassroomProposalResponse response = service.approve(proposal.getId(), manager.getEmail());

        assertThat(response.getApprovalStatus()).isEqualTo(ClassroomApprovalStatus.APPROVED);
        assertThat(response.getApprovedClassroomId()).isEqualTo(classroom.getId());
        assertThat(enrollmentRequest.getStatus()).isEqualTo(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        verify(classroomOfferingService, times(2)).createSession(
                any(),
                any(CreateClassroomSessionRequest.class)
        );
        verify(classroomOfferingService, never()).enrollStudent(any(), any());
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
        assertThat(enrollmentRequest.getStatus()).isEqualTo(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        verify(enrollmentHistoryRepository, never()).save(any());
    }

    private ClassroomProposal pendingProposal() {
        enrollmentRequest.setStatus(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        ClassroomProposal proposal = ClassroomProposal.builder()
                .id(50L)
                .proposalCode("CP-TEST")
                .title("IELTS Foundation A01")
                .courseOffering(courseOffering)
                .deliveryType(ClassroomDeliveryMode.OFFLINE)
                .placementLevel(null)
                .capacity(20)
                .plannedStartDate(LocalDate.of(2026, 8, 3))
                .plannedEndDate(LocalDate.of(2026, 8, 5))
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

    private CreateClassroomProposalRequest proposalPayload() {
        CreateClassroomProposalRequest payload = new CreateClassroomProposalRequest();
        payload.setTitle("IELTS Foundation A01");
        payload.setCourseOfferingId(courseOffering.getId());
        payload.setEnrollmentRequestIds(List.of());
        payload.setCapacity(20);
        payload.setPlannedStartDate(LocalDate.of(2026, 8, 3));
        payload.setPlannedEndDate(LocalDate.of(2026, 8, 5));
        payload.setWeekdays(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY));
        payload.setSessionStartTime(LocalTime.of(18, 30));
        payload.setSessionEndTime(LocalTime.of(20, 30));
        payload.setPrimaryTeacherId(teacher.getId());
        payload.setRoomId(room.getId());
        payload.setOfflineAddress("Cơ sở Cầu Giấy");
        return payload;
    }

    private User user(Long id, String email, RoleEnum role) {
        User user = User.builder().id(id).fullName(email).email(email).build();
        user.setRole(role);
        return user;
    }
}
