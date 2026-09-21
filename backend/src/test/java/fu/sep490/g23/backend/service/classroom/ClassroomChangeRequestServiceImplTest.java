package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.ConflictCheckRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateChangeRequestRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateClassroomSessionRequest;
import fu.sep490.g23.backend.dto.request.classroom.ReviewChangeRequestRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateCourseSuspensionRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomChangeRequestResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomChangeRequest;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomChangeRequestType;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.math.BigDecimal;
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
    @Mock private HomeworkAttachmentStorageService attachmentStorageService;

    private ClassroomChangeRequestServiceImpl service;
    private ClassSection offering;
    private ClassSchedule sourceSession;
    private User teacher;
    private User staff;
    private User secondStaff;
    private User manager;

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
                notificationService,
                attachmentStorageService
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
        secondStaff = User.builder()
                .id(100L)
                .fullName("Nhân viên thứ hai")
                .roles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.STAFF))
                .build();
        manager = User.builder()
                .id(101L)
                .fullName("Quản lý đào tạo")
                .roles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.MANAGER))
                .build();
    }

    @Test
    void createMakeupRequest_AcceptsCompletedSourceAndChecksOnlyProposedSchedule() {
        when(accessHelper.requireUser("teacher@example.com")).thenReturn(teacher);
        when(offeringRepository.findById(21L)).thenReturn(Optional.of(offering));
        when(sessionRepository.findById(31L)).thenReturn(Optional.of(sourceSession));
        when(userRepository.findEnabledByRoleCodeForUpdate(RoleCodes.STAFF))
                .thenReturn(List.of(staff, secondStaff));
        when(changeRequestRepository.findFirstByReviewerInOrderByCreatedAtDescIdDesc(List.of(staff, secondStaff)))
                .thenReturn(Optional.empty());

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
        ArgumentCaptor<ClassroomChangeRequest> requestCaptor = ArgumentCaptor.forClass(ClassroomChangeRequest.class);
        verify(changeRequestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getReviewer()).isEqualTo(staff);
        assertThat(requestCaptor.getValue().getReviewedAt()).isNull();
        verify(notificationService).notifyUser(
                eq(staff),
                eq("CLASSROOM_CHANGE_REQUEST_PENDING"),
                any(),
                any(),
                any()
        );
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
        when(accessHelper.requireUser("tm@example.com")).thenReturn(staff);
        when(changeRequestRepository.findById(1L)).thenReturn(Optional.of(pending));
        when(conflictService.check(any(ConflictCheckRequest.class)))
                .thenReturn(ConflictCheckResultResponse.builder()
                        .hasBlockingConflict(false)
                        .canOverride(false)
                        .conflicts(List.of())
                        .build());

        ConflictCheckResultResponse result = service.checkPendingConflict(1L, "tm@example.com");

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

    @Test
    void createRequest_AssignsNextStaffInRoundRobinOrder() {
        ClassroomChangeRequest previous = pendingMakeupRequest();
        previous.setReviewer(staff);
        when(accessHelper.requireUser("teacher@example.com")).thenReturn(teacher);
        when(offeringRepository.findById(21L)).thenReturn(Optional.of(offering));
        when(sessionRepository.findById(31L)).thenReturn(Optional.of(sourceSession));
        when(userRepository.findEnabledByRoleCodeForUpdate(RoleCodes.STAFF))
                .thenReturn(List.of(staff, secondStaff));
        when(changeRequestRepository.findFirstByReviewerInOrderByCreatedAtDescIdDesc(List.of(staff, secondStaff)))
                .thenReturn(Optional.of(previous));
        when(changeRequestRepository.save(any(ClassroomChangeRequest.class)))
                .thenAnswer(invocation -> {
                    ClassroomChangeRequest saved = invocation.getArgument(0);
                    saved.setId(52L);
                    return saved;
                });
        when(mapper.changeRequestTypeLabel(ClassroomChangeRequestType.CREATE_MAKEUP_SESSION))
                .thenReturn("Tạo buổi học bù");

        service.create(makeupRequest("""
                {
                  "sessionDate": "2026-07-20",
                  "startTime": "18:00",
                  "endTime": "20:00",
                  "teacherId": 41
                }
                """), "teacher@example.com");

        ArgumentCaptor<ClassroomChangeRequest> requestCaptor = ArgumentCaptor.forClass(ClassroomChangeRequest.class);
        verify(changeRequestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getReviewer()).isEqualTo(secondStaff);
    }

    @Test
    void listPending_OnlyReturnsRequestsAssignedToStaff() {
        ClassroomChangeRequest pending = pendingMakeupRequest();
        when(accessHelper.requireUser("tm@example.com")).thenReturn(staff);
        when(changeRequestRepository.findByReviewerAndStatusOrderByCreatedAtDesc(
                staff,
                ClassroomChangeRequestStatus.PENDING
        )).thenReturn(List.of(pending));

        service.listPending("tm@example.com");

        verify(changeRequestRepository).findByReviewerAndStatusOrderByCreatedAtDesc(
                staff,
                ClassroomChangeRequestStatus.PENDING
        );
        verify(changeRequestRepository, never()).findByStatusOrderByCreatedAtDesc(any());
    }

    @Test
    void listPending_ManagerKeepsOversightOfAllRequests() {
        when(accessHelper.requireUser("manager@example.com")).thenReturn(manager);
        when(changeRequestRepository.findByStatusOrderByCreatedAtDesc(ClassroomChangeRequestStatus.PENDING))
                .thenReturn(List.of(pendingMakeupRequest()));

        service.listPending("manager@example.com");

        verify(changeRequestRepository).findByStatusOrderByCreatedAtDesc(ClassroomChangeRequestStatus.PENDING);
        verify(changeRequestRepository, never()).findByReviewerAndStatusOrderByCreatedAtDesc(any(), any());
    }

    @Test
    void approveRequest_RejectsAnotherStaffOwner() {
        ClassroomChangeRequest pending = pendingMakeupRequest();
        when(accessHelper.requireUser("staff2@example.com")).thenReturn(secondStaff);
        when(changeRequestRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> service.approve(1L, new ReviewChangeRequestRequest(), "staff2@example.com"))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("nhân viên khác");

        verify(scheduleLockService, never()).lockDates(any());
        verify(changeRequestRepository, never()).save(any());
    }

    @Test
    void suspensionEligibility_AllowsExactlyHalfOfSessionsWhenTuitionIsPaid() {
        User learner = User.builder().id(70L).fullName("Học viên").build();
        ClassEnrollment enrollment = paidAssignedEnrollment(learner);
        LocalDateTime now = LocalDateTime.now();
        when(accessHelper.requireUser("learner@example.com")).thenReturn(learner);
        when(enrollmentRepository.findByStudentIdAndRegistrationStatusIn(
                70L, List.of(ClassroomRegistrationStatus.ASSIGNED))).thenReturn(List.of(enrollment));
        when(sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(21L))
                .thenReturn(List.of(sessionAt(now.minusDays(1)), sessionAt(now.plusDays(1))));
        when(changeRequestRepository.findByRequesterIdAndRequestTypeInOrderByCreatedAtDesc(
                70L, List.of(ClassroomChangeRequestType.SUSPEND_STUDENT))).thenReturn(List.of());

        var result = service.listSuspensionEligibility("learner@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isEligible()).isTrue();
        assertThat(result.get(0).getProgressPercent()).isEqualTo(50);
    }

    @Test
    void createSuspensionRequest_RejectsProgressAboveHalf() {
        User learner = User.builder().id(70L).fullName("Học viên").build();
        ClassEnrollment enrollment = paidAssignedEnrollment(learner);
        LocalDateTime now = LocalDateTime.now();
        when(accessHelper.requireUser("learner@example.com")).thenReturn(learner);
        when(userRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(learner));
        when(enrollmentRepository.findByIdForUpdate(81L)).thenReturn(Optional.of(enrollment));
        when(sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(21L))
                .thenReturn(List.of(
                        sessionAt(now.minusDays(2)),
                        sessionAt(now.minusDays(1)),
                        sessionAt(now.plusDays(1))
                ));

        CreateCourseSuspensionRequest request = new CreateCourseSuspensionRequest();
        request.setEnrollmentId(81L);
        request.setRequestedStartDate(LocalDate.now());
        request.setRequestedReturnDate(LocalDate.now().plusMonths(2));
        request.setReason("Điều trị chấn thương theo chỉ định bác sĩ.");
        request.setProofUrl("/api/classroom-homework/attachments/proof.pdf");

        assertThatThrownBy(() -> service.createSuspensionRequest(request, "learner@example.com"))
                .hasMessage("Khóa học đã vượt quá 50% số buổi.");
        verify(changeRequestRepository, never()).save(any());
    }

    @Test
    void createSuspensionRequest_RejectsDurationLongerThanThreeMonths() {
        User learner = User.builder().id(70L).fullName("Học viên").build();
        ClassEnrollment enrollment = paidAssignedEnrollment(learner);
        LocalDateTime now = LocalDateTime.now();
        when(accessHelper.requireUser("learner@example.com")).thenReturn(learner);
        when(userRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(learner));
        when(enrollmentRepository.findByIdForUpdate(81L)).thenReturn(Optional.of(enrollment));
        when(sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(21L))
                .thenReturn(List.of(sessionAt(now.minusDays(1)), sessionAt(now.plusDays(1))));
        when(changeRequestRepository.findByRequesterIdAndRequestTypeInOrderByCreatedAtDesc(
                70L, List.of(ClassroomChangeRequestType.SUSPEND_STUDENT))).thenReturn(List.of());

        CreateCourseSuspensionRequest request = new CreateCourseSuspensionRequest();
        request.setEnrollmentId(81L);
        request.setRequestedStartDate(LocalDate.now());
        request.setRequestedReturnDate(LocalDate.now().plusMonths(3).plusDays(1));
        request.setReason("Điều trị chấn thương theo chỉ định bác sĩ.");
        request.setProofUrl("/api/classroom-homework/attachments/proof.pdf");

        assertThatThrownBy(() -> service.createSuspensionRequest(request, "learner@example.com"))
                .hasMessage("Thời gian bảo lưu không được vượt quá 3 tháng.");
        verify(changeRequestRepository, never()).save(any());
    }

    @Test
    void createSuspensionRequest_StoresCanonicalInternalProofUrl() {
        User learner = User.builder()
                .id(70L)
                .fullName("Học viên")
                .roles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.LEARNER))
                .build();
        ClassEnrollment enrollment = paidAssignedEnrollment(learner);
        LocalDateTime now = LocalDateTime.now();
        when(accessHelper.requireUser("learner@example.com")).thenReturn(learner);
        when(userRepository.findByIdForUpdate(70L)).thenReturn(Optional.of(learner));
        when(enrollmentRepository.findByIdForUpdate(81L)).thenReturn(Optional.of(enrollment));
        when(sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(21L))
                .thenReturn(List.of(sessionAt(now.minusDays(1)), sessionAt(now.plusDays(1))));
        when(changeRequestRepository.findByRequesterIdAndRequestTypeInOrderByCreatedAtDesc(
                70L, List.of(ClassroomChangeRequestType.SUSPEND_STUDENT))).thenReturn(List.of());
        when(attachmentStorageService.loadStoredAttachmentFromUrl("https://files.example/homework-proof.pdf"))
                .thenReturn(Optional.of(new HomeworkAttachmentStorageService.StoredHomeworkAttachment(
                        "homework-proof.pdf", "application/pdf", 3, new byte[] {1, 2, 3}
                )));
        when(userRepository.findEnabledByRoleCodeForUpdate(RoleCodes.STAFF)).thenReturn(List.of(staff));
        when(changeRequestRepository.save(any(ClassroomChangeRequest.class))).thenAnswer(invocation -> {
            ClassroomChangeRequest saved = invocation.getArgument(0);
            saved.setId(92L);
            return saved;
        });

        CreateCourseSuspensionRequest request = new CreateCourseSuspensionRequest();
        request.setEnrollmentId(81L);
        request.setRequestedStartDate(LocalDate.now());
        request.setRequestedReturnDate(LocalDate.now().plusMonths(2));
        request.setReason("Điều trị chấn thương theo chỉ định bác sĩ.");
        request.setProofUrl("https://files.example/homework-proof.pdf");

        service.createSuspensionRequest(request, "learner@example.com");

        ArgumentCaptor<ClassroomChangeRequest> captor = ArgumentCaptor.forClass(ClassroomChangeRequest.class);
        verify(changeRequestRepository).save(captor.capture());
        assertThat(captor.getValue().getNewValuesJson())
                .contains("/api/classroom-homework/attachments/homework-proof.pdf")
                .doesNotContain("files.example");
    }

    @Test
    void approveSuspension_RevalidatesAndRemovesClassAccess() {
        User learner = User.builder().id(70L).fullName("Học viên").build();
        ClassEnrollment enrollment = paidAssignedEnrollment(learner);
        LocalDateTime now = LocalDateTime.now();
        ClassroomChangeRequest request = ClassroomChangeRequest.builder()
                .id(91L)
                .requestType(ClassroomChangeRequestType.SUSPEND_STUDENT)
                .requester(learner)
                .classSection(enrollment.getClassSection())
                .newValuesJson("{\"enrollmentId\":81,\"requestedStartDate\":\""
                        + LocalDate.now() + "\",\"requestedReturnDate\":\""
                        + LocalDate.now().plusMonths(2) + "\"}")
                .reason("Điều trị chấn thương")
                .status(ClassroomChangeRequestStatus.PENDING)
                .reviewer(staff)
                .build();
        when(accessHelper.requireUser("staff@example.com")).thenReturn(staff);
        when(changeRequestRepository.findByIdForUpdate(91L)).thenReturn(Optional.of(request));
        when(enrollmentRepository.findByIdForUpdate(81L)).thenReturn(Optional.of(enrollment));
        when(sessionRepository.findByClassSectionIdOrderBySessionDateAscStartTimeAsc(21L))
                .thenReturn(List.of(sessionAt(now.minusDays(1)), sessionAt(now.plusDays(1))));
        when(changeRequestRepository.findByRequesterIdAndRequestTypeInOrderByCreatedAtDesc(
                70L, List.of(ClassroomChangeRequestType.SUSPEND_STUDENT))).thenReturn(List.of());
        when(changeRequestRepository.save(any(ClassroomChangeRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.approve(91L, new ReviewChangeRequestRequest(), "staff@example.com");

        verify(offeringService).suspendEnrollment(81L, "Bảo lưu theo yêu cầu #91");
        verify(scheduleLockService, never()).lockDates(any());
    }

    private ClassEnrollment paidAssignedEnrollment(User learner) {
        InstructorLedCourse course = InstructorLedCourse.builder()
                .id(61L)
                .title("IELTS Foundation")
                .build();
        ClassSection classroom = ClassSection.builder()
                .id(21L)
                .name("IELTS Foundation Evening")
                .instructorLedCourse(course)
                .build();
        return ClassEnrollment.builder()
                .id(81L)
                .student(learner)
                .classSection(classroom)
                .registrationStatus(ClassroomRegistrationStatus.ASSIGNED)
                .agreedTuitionFeeVnd(BigDecimal.valueOf(5_000_000))
                .tuitionAmountDue(BigDecimal.valueOf(5_000_000))
                .tuitionAmountPaid(BigDecimal.valueOf(5_000_000))
                .build();
    }

    private ClassSchedule sessionAt(LocalDateTime dateTime) {
        return ClassSchedule.builder()
                .classSection(offering)
                .sessionDate(dateTime.toLocalDate())
                .startTime(dateTime.toLocalTime().minusHours(1))
                .endTime(dateTime.toLocalTime())
                .status(ClassroomSessionStatus.SCHEDULED)
                .build();
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
                .reviewer(staff)
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
