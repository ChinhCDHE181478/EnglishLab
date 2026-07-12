package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateChangeRequestRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateClassroomSessionRequest;
import fu.sap490.g23.backend.dto.request.classroom.ReviewChangeRequestRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomChangeRequestResponse;
import fu.sap490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomChangeRequestRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomRoomRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.classroom.impl.ClassroomChangeRequestServiceImpl;
import fu.sap490.g23.backend.service.notification.ClassroomNotificationService;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomChangeRequestServiceImplTest {

    @Mock private ClassroomChangeRequestRepository changeRequestRepository;
    @Mock private ClassroomOfferingRepository offeringRepository;
    @Mock private ClassroomEnrollmentRepository enrollmentRepository;
    @Mock private ClassroomSessionRepository sessionRepository;
    @Mock private ClassroomRoomRepository roomRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClassroomMapper mapper;
    @Mock private ClassroomConflictService conflictService;
    @Mock private ClassroomOfferingService offeringService;
    @Mock private ClassroomAccessHelper accessHelper;
    @Mock private ClassroomNotificationService notificationService;

    private ClassroomChangeRequestServiceImpl service;
    private ClassroomOffering offering;
    private ClassroomSession sourceSession;
    private User teacher;
    private User trainingManager;

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
                offeringService,
                accessHelper,
                notificationService
        );

        offering = ClassroomOffering.builder().id(21L).build();
        sourceSession = ClassroomSession.builder()
                .id(31L)
                .classroomOffering(offering)
                .sessionDate(LocalDate.of(2026, 7, 1))
                .startTime(LocalTime.of(18, 0))
                .endTime(LocalTime.of(20, 0))
                .status(ClassroomSessionStatus.COMPLETED)
                .locked(true)
                .build();
        teacher = User.builder()
                .id(41L)
                .fullName("Teacher Test")
                .role(RoleEnum.TEACHER)
                .build();
        trainingManager = User.builder()
                .id(99L)
                .fullName("Training Manager")
                .role(RoleEnum.TRAINING_MANAGER)
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
        when(accessHelper.requireUser("tm@example.com")).thenReturn(trainingManager);
        when(changeRequestRepository.findById(1L)).thenReturn(Optional.of(pending));
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
        verify(offeringService).createSession(eq(21L), any(CreateClassroomSessionRequest.class), eq(true));
        assertThat(response.getStatus()).isEqualTo(ClassroomChangeRequestStatus.APPLIED);
    }

    private ClassroomChangeRequest pendingMakeupRequest() {
        return ClassroomChangeRequest.builder()
                .id(1L)
                .requestType(ClassroomChangeRequestType.CREATE_MAKEUP_SESSION)
                .requester(teacher)
                .classroomOffering(offering)
                .targetSession(sourceSession)
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
                .classroomOfferingId(21L)
                .targetSessionId(31L)
                .newValuesJson(newValuesJson)
                .reason("Tổ chức buổi học bù")
                .build();
    }
}
