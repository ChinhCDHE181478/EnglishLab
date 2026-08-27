package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateChangeRequestRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomSessionRequest;
import fu.sep490.g23.backend.dto.request.classroom.ReviewChangeRequestRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomChangeRequestResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomChangeRequestRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.RoomRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.impl.ClassroomChangeRequestServiceImpl;
import fu.sep490.g23.backend.service.notification.ClassroomNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomChangeRequestServiceImplTest {

    @Mock private ClassroomChangeRequestRepository changeRequestRepository;
    @Mock private ClassSectionRepository offeringRepository;
    @Mock private ClassEnrollmentRepository enrollmentRepository;
    @Mock private ClassScheduleRepository sessionRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClassroomMapper mapper;
    @Mock private ClassroomConflictService conflictService;
    @Mock private ClassroomScheduleLockService scheduleLockService;
    @Mock private ClassroomOfferingService offeringService;
    @Mock private ClassroomAccessHelper accessHelper;
    @Mock private ClassroomNotificationService notificationService;

    private ClassroomChangeRequestServiceImpl service;
    private ClassSection offering;
    private ClassSchedule sourceSession;
    private User teacher;
    private User staff;

    @BeforeEach
    void setUp() {
        service = new ClassroomChangeRequestServiceImpl(
                changeRequestRepository,
                offeringRepository,
                enrollmentRepository,
                sessionRepository,
                roomRepository,
                userRepository,
                mapper,
                conflictService,
                scheduleLockService,
                offeringService,
                accessHelper,
                notificationService
        );

        offering = ClassSection.builder().id(21L).build();
        sourceSession = ClassSchedule.builder()
                .id(31L)
                .classSection(offering)
                .sessionDate(LocalDate.of(2026, 7, 1))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 0))
                .status(ClassroomSessionStatus.COMPLETED)
                .build();
        teacher = User.builder()
                .id(41L)
                .fullName("Teacher Test")
                .roles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.TEACHER))
                .build();
        staff = User.builder()
                .id(99L)
                .fullName("Nhân viên đào tạo")
                .roles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.STAFF))
                .build();
    }

    @Test
    void createMakeupRequest_AcceptsCompletedSourceAndChecksOnlyProposedSchedule() {
        when(accessHelper.requireUser("teacher@example.com")).thenReturn(teacher);
        when(offeringRepository.findById(21L)).thenReturn(Optional.of(offering));
        when(sessionRepository.findById(31L)).thenReturn(Optional.of(sourceSession));

        CreateChangeRequestRequest request = makeupRequest("""
                {
                  "sessionDate": "2026-07-20",
                  "startTime": "18:00",
                  "endTime": "20:00",
                  "teacherId": 41
                }
                """);

        when(changeRequestRepository.save(any(ClassroomChangeRequest.class))).thenAnswer(invocation -> {
            ClassroomChangeRequest saved = invocation.getArgument(0);
            saved.setId(51L);
            return saved;
        });
        when(mapper.changeRequestTypeLabel(ClassroomChangeRequestType.CREATE_MAKEUP_SESSION))
                .thenReturn("Tạo buổi học bù");
        when(mapper.toChangeRequestResponse(any(ClassroomChangeRequest.class)))
                .thenReturn(ClassroomChangeRequestResponse.builder().id(51L).build());

        ClassroomChangeRequestResponse response = service.create(request, "teacher@example.com");

        ArgumentCaptor<ConflictCheckRequest> conflictCaptor = ArgumentCaptor.forClass(ConflictCheckRequest.class);
        verify(conflictService).assertNoBlockingConflict(conflictCaptor.capture());
        assertThat(conflictCaptor.getValue().getCheckSessionLocked()).isFalse();
        assertThat(conflictCaptor.getValue().getSessionDate()).isEqualTo(LocalDate.of(2026, 7, 20));
        assertThat(response.getId()).isEqualTo(51L);
    }

    @Test
    void createMakeupRequest_RejectsMissingSchedule() {
        when(accessHelper.requireUser("teacher@example.com")).thenReturn(teacher);
        when(offeringRepository.findById(21L)).thenReturn(Optional.of(offering));
        when(sessionRepository.findById(31L)).thenReturn(Optional.of(sourceSession));

        CreateChangeRequestRequest request = makeupRequest("{}");

        assertThatThrownBy(() -> service.create(request, "teacher@example.com"))
                .hasMessage("Vui lòng chọn ngày học bù.");

        verify(conflictService, never()).assertNoBlockingConflict(any());
        verify(changeRequestRepository, never()).save(any());
    }

    @Test
    void checkPendingMakeupConflict_DoesNotTreatCompletedSourceAsLocked() {
        ClassroomChangeRequest pending = pendingMakeupRequest();
        when(changeRequestRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(conflictService.check(any(ConflictCheckRequest.class)))
                .thenReturn(ConflictCheckResultResponse.builder()
                        .hasBlockingConflict(false)
                        .canOverride(false)
                        .conflicts(List.of())
                        .build());

        ConflictCheckResultResponse result = service.checkPendingConflict(1L);

        ArgumentCaptor<ConflictCheckRequest> conflictCaptor = ArgumentCaptor.forClass(ConflictCheckRequest.class);
        verify(conflictService).check(conflictCaptor.capture());
        assertThat(conflictCaptor.getValue().getCheckSessionLocked()).isFalse();
        assertThat(result.isHasBlockingConflict()).isFalse();
    }

    @Test
    void approveMakeupRequest_CreatesMakeupSessionWithoutSessionLockedGate() {
        ClassroomChangeRequest pending = pendingMakeupRequest();
        when(accessHelper.requireUser("tm@example.com")).thenReturn(staff);
        when(changeRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pending));
        when(changeRequestRepository.save(any(ClassroomChangeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.changeRequestTypeLabel(ClassroomChangeRequestType.CREATE_MAKEUP_SESSION))
                .thenReturn("Tạo buổi học bù");
        when(mapper.toChangeRequestResponse(any(ClassroomChangeRequest.class)))
                .thenReturn(ClassroomChangeRequestResponse.builder().id(1L).status(ClassroomChangeRequestStatus.APPLIED).build());

        ReviewChangeRequestRequest review = new ReviewChangeRequestRequest();
        review.setOverrideConflict(false);

        ClassroomChangeRequestResponse response = service.approve(1L, review, "tm@example.com");

        ArgumentCaptor<ConflictCheckRequest> conflictCaptor = ArgumentCaptor.forClass(ConflictCheckRequest.class);
        verify(conflictService).assertNoBlockingConflict(conflictCaptor.capture());
        assertThat(conflictCaptor.getValue().getCheckSessionLocked()).isFalse();
        var approvalOrder = inOrder(scheduleLockService, conflictService, offeringService);
        approvalOrder.verify(scheduleLockService).lockDates(List.of(
                LocalDate.of(2026, 7, 20),
                LocalDate.of(2026, 7, 1)
        ));
        approvalOrder.verify(conflictService).assertNoBlockingConflict(any(ConflictCheckRequest.class));
        approvalOrder.verify(offeringService).createSession(eq(21L), any(CreateClassroomSessionRequest.class), eq(true));
        assertThat(response.getStatus()).isEqualTo(ClassroomChangeRequestStatus.APPLIED);
    }

    @Test
    void approveRequest_RejectsRequestThatWasAlreadyReviewedUnderRowLock() {
        ClassroomChangeRequest reviewed = pendingMakeupRequest();
        reviewed.setStatus(ClassroomChangeRequestStatus.APPLIED);
        when(accessHelper.requireUser("tm@example.com")).thenReturn(staff);
        when(changeRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(reviewed));

        assertThatThrownBy(() -> service.approve(1L, new ReviewChangeRequestRequest(), "tm@example.com"))
                .hasMessage("Yêu cầu không còn ở trạng thái chờ duyệt.");

        verify(scheduleLockService, never()).lockDates(any());
        verify(offeringService, never()).createSession(any(), any(), eq(true));
    }

    private ClassroomChangeRequest pendingMakeupRequest() {
        return ClassroomChangeRequest.builder()
                .id(1L)
                .requestType(ClassroomChangeRequestType.CREATE_MAKEUP_SESSION)
                .requester(teacher)
                .classSection(offering)
                .targetClassSchedule(sourceSession)
                .newValuesJson("""
                        {
                          "sessionDate": "2026-07-20",
                          "startTime": "18:00",
                          "endTime": "20:00",
                          "teacherId": 41
                        }
                        """)
                .reason("Tổ chức buổi học bù")
                .status(ClassroomChangeRequestStatus.PENDING)
                .build();
    }

    private CreateChangeRequestRequest makeupRequest(String newValuesJson) {
        return CreateChangeRequestRequest.builder()
                .requestType(ClassroomChangeRequestType.CREATE_MAKEUP_SESSION)
                .classSectionId(21L)
                .targetSessionId(31L)
                .newValuesJson(newValuesJson)
                .reason("Tổ chức buổi học bù")
                .build();
    }
}
