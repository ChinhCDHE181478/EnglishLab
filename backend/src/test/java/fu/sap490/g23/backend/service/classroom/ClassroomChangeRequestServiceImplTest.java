package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.ReviewChangeRequestRequest;
import fu.sap490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.ClassroomSession;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.*;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.classroom.impl.ClassroomChangeRequestServiceImpl;
import fu.sap490.g23.backend.service.notification.ClassroomNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
    }

    @Test
    void checkPendingConflict_returnsConflictServiceResult() {
        ClassroomChangeRequest changeRequest = pendingRescheduleRequest();
        when(changeRequestRepository.findById(1L)).thenReturn(Optional.of(changeRequest));
        ConflictCheckResultResponse expected = ConflictCheckResultResponse.builder()
                .hasBlockingConflict(true)
                .conflicts(List.of())
                .build();
        when(conflictService.check(any())).thenReturn(expected);

        ConflictCheckResultResponse result = service.checkPendingConflict(1L);

        assertTrue(result.isHasBlockingConflict());
        verify(conflictService).check(any());
    }

    @Test
    void approve_withoutOverride_checksConflictBeforeApply() {
        User reviewer = trainingManager();
        ClassroomChangeRequest changeRequest = pendingRescheduleRequest();
        when(accessHelper.requireUser("tm@englishlab.vn")).thenReturn(reviewer);
        when(changeRequestRepository.findById(1L)).thenReturn(Optional.of(changeRequest));
        when(changeRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toChangeRequestResponse(any())).thenReturn(null);
        when(mapper.changeRequestTypeLabel(any())).thenReturn("Đổi lịch buổi học");

        ReviewChangeRequestRequest request = ReviewChangeRequestRequest.builder()
                .reviewNote("Đồng ý")
                .overrideConflict(false)
                .build();

        service.approve(1L, request, "tm@englishlab.vn");

        verify(conflictService).assertNoBlockingConflict(any());
        verify(offeringService).applyApprovedSessionScheduleChange(eq(10L), any());
        verify(changeRequestRepository).save(argThat(saved -> saved.getStatus() == ClassroomChangeRequestStatus.APPLIED));
    }

    @Test
    void approve_withOverride_skipsConflictCheckButRequiresNote() {
        User reviewer = trainingManager();
        ClassroomChangeRequest changeRequest = pendingRescheduleRequest();
        when(accessHelper.requireUser("tm@englishlab.vn")).thenReturn(reviewer);
        when(changeRequestRepository.findById(1L)).thenReturn(Optional.of(changeRequest));
        when(changeRequestRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toChangeRequestResponse(any())).thenReturn(null);
        when(mapper.changeRequestTypeLabel(any())).thenReturn("Đổi lịch buổi học");

        ReviewChangeRequestRequest request = ReviewChangeRequestRequest.builder()
                .reviewNote("Ghi đè theo quyết định vận hành")
                .overrideConflict(true)
                .build();

        service.approve(1L, request, "tm@englishlab.vn");

        verify(conflictService, never()).assertNoBlockingConflict(any());
        verify(offeringService).applyApprovedSessionScheduleChange(eq(10L), any());
    }

    @Test
    void approve_withOverride_withoutNote_throws() {
        User reviewer = trainingManager();
        ClassroomChangeRequest changeRequest = pendingRescheduleRequest();
        when(accessHelper.requireUser("tm@englishlab.vn")).thenReturn(reviewer);
        when(changeRequestRepository.findById(1L)).thenReturn(Optional.of(changeRequest));

        ReviewChangeRequestRequest request = ReviewChangeRequestRequest.builder()
                .overrideConflict(true)
                .build();

        assertThrows(RuntimeException.class, () -> service.approve(1L, request, "tm@englishlab.vn"));
        verify(offeringService, never()).applyApprovedSessionScheduleChange(anyLong(), any());
    }

    private User trainingManager() {
        User user = new User();
        user.setId(99L);
        user.setEmail("tm@englishlab.vn");
        user.setRole(RoleEnum.TRAINING_MANAGER);
        return user;
    }

    private ClassroomChangeRequest pendingRescheduleRequest() {
        LearningPackage learningPackage = LearningPackage.builder().title("IELTS Foundation").build();
        ClassroomOffering offering = ClassroomOffering.builder()
                .id(5L)
                .learningPackage(learningPackage)
                .build();
        ClassroomSession session = ClassroomSession.builder()
                .id(10L)
                .classroomOffering(offering)
                .sessionDate(LocalDate.now().plusDays(3))
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(16, 0))
                .status(ClassroomSessionStatus.SCHEDULED)
                .build();
        User teacher = new User();
        teacher.setId(7L);
        teacher.setFullName("Teacher Demo");
        return ClassroomChangeRequest.builder()
                .id(1L)
                .requestType(ClassroomChangeRequestType.RESCHEDULE_SESSION)
                .requester(teacher)
                .classroomOffering(offering)
                .targetSession(session)
                .oldValuesJson("{\"sessionDate\":\"" + session.getSessionDate() + "\"}")
                .newValuesJson("{\"sessionDate\":\"" + session.getSessionDate().plusDays(1)
                        + "\",\"startTime\":\"14:00\",\"endTime\":\"16:00\"}")
                .reason("Đổi lịch")
                .status(ClassroomChangeRequestStatus.PENDING)
                .build();
    }
}
