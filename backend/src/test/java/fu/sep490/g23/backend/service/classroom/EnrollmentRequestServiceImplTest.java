package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.AssignEnrollmentClassRequest;
import fu.sep490.g23.backend.dto.request.classroom.CompleteEnrollmentTestRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateCenterEnrollmentRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateCourseEnrollmentRequest;
import fu.sep490.g23.backend.dto.request.classroom.ScheduleEnrollmentTestRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sep490.g23.backend.dto.response.classroom.CourseEnrollmentRequestResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.AuthToken;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.EnrollmentRequest;
import fu.sep490.g23.backend.entity.classroom.TrainingProgram;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomSessionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.EnrollmentRequestRepository;
import fu.sep490.g23.backend.repository.classroom.EnrollmentRequestStatusHistoryRepository;
import fu.sep490.g23.backend.repository.classroom.TrainingProgramRepository;
import fu.sep490.g23.backend.service.assessment.PlacementEligibilityService;
import fu.sep490.g23.backend.service.auth.AuthTokenService;
import fu.sep490.g23.backend.service.classroom.impl.EnrollmentRequestServiceImpl;
import fu.sep490.g23.backend.service.mail.EnrollmentRequestMailService;
import fu.sep490.g23.backend.service.mail.AuthMailService;
import fu.sep490.g23.backend.service.user.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentRequestServiceImplTest {
    @Mock private EnrollmentRequestRepository requestRepository;
    @Mock private EnrollmentRequestStatusHistoryRepository historyRepository;
    @Mock private TrainingProgramRepository trainingProgramRepository;
    @Mock private ClassroomOfferingRepository classroomOfferingRepository;
    @Mock private ClassroomSessionRepository classroomSessionRepository;
    @Mock private ClassroomEnrollmentRepository classroomEnrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlacementEligibilityService placementEligibilityService;
    @Mock private ClassroomOfferingService classroomOfferingService;
    @Mock private ClassroomConflictService classroomConflictService;
    @Mock private EnrollmentRequestMailService enrollmentRequestMailService;
    @Mock private AuthTokenService authTokenService;
    @Mock private AuthMailService authMailService;
    @Mock private UserRoleService userRoleService;
    @Mock private PasswordEncoder passwordEncoder;

    private EnrollmentRequestServiceImpl service;
    private User learner;
    private User staff;
    private User manager;
    private TrainingProgram program;
    private ClassroomOffering classroom;

    @BeforeEach
    void setUp() {
        service = new EnrollmentRequestServiceImpl(
                requestRepository,
                historyRepository,
                trainingProgramRepository,
                classroomOfferingRepository,
                classroomSessionRepository,
                classroomEnrollmentRepository,
                userRepository,
                placementEligibilityService,
                classroomOfferingService,
                classroomConflictService,
                enrollmentRequestMailService,
                authTokenService,
                authMailService,
                userRoleService,
                passwordEncoder
        );
        learner = user(10L, "learner@example.com", RoleEnum.LEARNER);
        staff = user(50L, "staff@example.com", RoleEnum.STAFF);
        manager = user(60L, "manager@example.com", RoleEnum.MANAGER);
        program = TrainingProgram.builder()
                .id(20L)
                .code("IELTS-FOUNDATION")
                .title("IELTS Foundation")
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .status(PackageStatus.PUBLISHED)
                .maxCapacity(24)
                .build();
        LearningPackage learningPackage = LearningPackage.builder()
                .id(21L)
                .title("IELTS F01")
                .slug("ielts-f01")
                .studyMode("T2, T4, T6 · 18:30")
                .status(PackageStatus.PUBLISHED)
                .build();
        classroom = ClassroomOffering.builder()
                .id(30L)
                .learningPackage(learningPackage)
                .trainingProgram(program)
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .status(ClassroomOfferingStatus.UPCOMING)
                .startDate(LocalDate.now().plusDays(14))
                .build();
    }

    @Test
    void learnerRegistersInterestInCourseInsteadOfClass() {
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(trainingProgramRepository.findById(program.getId())).thenReturn(Optional.of(program));
        when(requestRepository.existsByLearnerAndCourseOfferingAndStatusNotIn(any(), any(), anySet()))
                .thenReturn(false);
        stubPersistence();

        CourseEnrollmentRequestResponse response = service.submit(consultationForm(), learner.getEmail());

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.SUBMITTED);
        assertThat(response.getCourseOfferingId()).isEqualTo(program.getId());
        assertThat(response.getRequestedClassroomId()).isNull();
        verify(historyRepository).save(any());
        verify(requestRepository).existsByLearnerAndCourseOfferingAndStatusNotIn(
                eq(learner),
                eq(program),
                anySet()
        );
    }

    @Test
    void learnerCannotCreateSecondActiveRequestForSameCourse() {
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(trainingProgramRepository.findById(program.getId())).thenReturn(Optional.of(program));
        when(requestRepository.existsByLearnerAndCourseOfferingAndStatusNotIn(
                eq(learner),
                eq(program),
                anySet()
        )).thenReturn(true);

        assertThatThrownBy(() -> service.submit(consultationForm(), learner.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khóa học này");
    }

    @Test
    void learnerCannotRegisterDirectlyForClass() {
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        CreateCourseEnrollmentRequest payload = consultationForm();
        payload.setClassroomId(classroom.getId());

        assertThatThrownBy(() -> service.submit(payload, learner.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chỉ chọn khóa học");
    }

    @Test
    void staffSchedulesFutureTestFromNewRequestAndSendsConfirmation() {
        EnrollmentRequest request = enrollmentRequest(EnrollmentRequestStatus.SUBMITTED);
        stubStaffRequest(request);
        stubPersistence();
        ScheduleEnrollmentTestRequest payload = new ScheduleEnrollmentTestRequest();
        payload.setAppointmentAt(LocalDateTime.now().plusDays(2).withSecond(0).withNano(0));
        payload.setLocation("EnglishLab Campus");

        CourseEnrollmentRequestResponse response = service.scheduleTest(request.getId(), payload, staff.getEmail());

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.TEST_SCHEDULED);
        assertThat(response.getInvitationSentAt()).isNotNull();
        assertThat(response.getTestLocation()).isEqualTo("EnglishLab Campus");
        verify(enrollmentRequestMailService).sendTestAppointment(request);
    }

    @Test
    void staffCanRecordResultWhenLearnerArrivesBeforeAppointment() {
        EnrollmentRequest request = enrollmentRequest(EnrollmentRequestStatus.TEST_SCHEDULED);
        request.setTestAppointmentAt(LocalDateTime.now().plusHours(2));
        stubStaffRequest(request);
        stubPersistence();

        CourseEnrollmentRequestResponse response = service.completeTest(
                request.getId(),
                eligibleResult(),
                staff.getEmail()
        );

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        assertThat(response.getTestCompletedAt()).isNotNull();
    }

    @Test
    void passedTestMovesLearnerToWaitingForClass() {
        EnrollmentRequest request = enrollmentRequest(EnrollmentRequestStatus.TEST_SCHEDULED);
        request.setTestAppointmentAt(LocalDateTime.now().minusHours(1));
        stubStaffRequest(request);
        stubPersistence();

        CourseEnrollmentRequestResponse response = service.completeTest(request.getId(), eligibleResult(), staff.getEmail());

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        assertThat(response.getConfirmedLevel()).isEqualTo(PlacementLevel.INTERMEDIATE);
        assertThat(response.getTestCompletedAt()).isNotNull();
    }

    @Test
    void staffCanAssignAClassFromDifferentCourseAfterPlacementTestAndEmailsLearner() {
        EnrollmentRequest request = enrollmentRequest(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        TrainingProgram placementProgram = TrainingProgram.builder()
                .id(99L)
                .code("IELTS-INTERMEDIATE")
                .title("IELTS Intermediate")
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .status(PackageStatus.PUBLISHED)
                .build();
        classroom.setTrainingProgram(placementProgram);
        stubStaffRequest(request);
        when(classroomOfferingRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(classroomOfferingService.enrollStudent(any(), any()))
                .thenReturn(ClassroomEnrollmentResponse.builder().hasClassAccess(true).build());
        stubPersistence();
        AssignEnrollmentClassRequest payload = new AssignEnrollmentClassRequest();
        payload.setClassroomId(classroom.getId());

        CourseEnrollmentRequestResponse response = service.assignClass(request.getId(), payload, staff.getEmail());

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.CLASS_ASSIGNED);
        assertThat(response.getAssignedClassroomId()).isEqualTo(classroom.getId());
        verify(enrollmentRequestMailService).sendClassAssignment(request, classroom);
    }

    @Test
    void staffCannotAssignBeforeTestIsCompleted() {
        EnrollmentRequest request = enrollmentRequest(EnrollmentRequestStatus.TEST_SCHEDULED);
        stubStaffRequest(request);
        AssignEnrollmentClassRequest payload = new AssignEnrollmentClassRequest();
        payload.setClassroomId(classroom.getId());

        assertThatThrownBy(() -> service.assignClass(request.getId(), payload, staff.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã test");
    }

    @Test
    void staffCreatesLearnerAccountAndAssignsClassForCenterRegistration() {
        CreateCenterEnrollmentRequest payload = centerEnrollment();
        AuthToken setupToken = AuthToken.builder().token("123456").build();
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(classroomOfferingRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(userRepository.findByEmail("new.learner@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(any())).thenReturn("encoded-random-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            saved.setId(70L);
            saved.setRole(RoleEnum.LEARNER);
            return saved;
        });
        when(classroomEnrollmentRepository
                .existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(any(), any(), anySet()))
                .thenReturn(false);
        when(classroomOfferingService.enrollStudent(eq(classroom.getId()), any()))
                .thenReturn(ClassroomEnrollmentResponse.builder().hasClassAccess(true).build());
        when(authTokenService.issuePasswordResetToken(any())).thenReturn(setupToken);
        stubPersistence();

        CourseEnrollmentRequestResponse response = service.createAtCenter(payload, staff.getEmail());

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.CLASS_ASSIGNED);
        assertThat(response.getRequestSource().name()).isEqualTo("CENTER");
        assertThat(response.isLearnerAccountCreated()).isTrue();
        assertThat(response.isAccountSetupEmailSent()).isTrue();
        assertThat(response.getAssignedClassroomId()).isEqualTo(classroom.getId());
        verify(userRoleService).assignRole(any(User.class), eq(RoleEnum.LEARNER));
        verify(authMailService).sendStaffCreatedAccountEmail(any(User.class), eq("123456"));
        verify(enrollmentRequestMailService).sendClassAssignment(any(EnrollmentRequest.class), eq(classroom));
    }

    @Test
    void staffCannotCreateDuplicateCenterEnrollmentInSameClass() {
        CreateCenterEnrollmentRequest payload = centerEnrollment();
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(classroomOfferingRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(userRepository.findByEmail("new.learner@example.com")).thenReturn(Optional.of(learner));
        when(userRepository.save(learner)).thenReturn(learner);
        when(classroomEnrollmentRepository
                .existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(any(), any(), anySet()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.createAtCenter(payload, staff.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("đã có hồ sơ");
    }

    @Test
    void staffReusesExistingLearnerAccountWithoutChangingItsPassword() {
        CreateCenterEnrollmentRequest payload = centerEnrollment();
        payload.setEmail(learner.getEmail());
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(classroomOfferingRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(userRepository.save(learner)).thenReturn(learner);
        when(classroomEnrollmentRepository
                .existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(any(), any(), anySet()))
                .thenReturn(false);
        when(classroomOfferingService.enrollStudent(eq(classroom.getId()), any()))
                .thenReturn(ClassroomEnrollmentResponse.builder().hasClassAccess(true).build());
        stubPersistence();

        CourseEnrollmentRequestResponse response = service.createAtCenter(payload, staff.getEmail());

        assertThat(response.isLearnerAccountCreated()).isFalse();
        assertThat(response.isAccountSetupEmailSent()).isFalse();
        verify(passwordEncoder, never()).encode(any());
        verify(authTokenService, never()).issuePasswordResetToken(any());
    }

    @Test
    void staffCannotUseInternalAccountAsLearnerAtCenter() {
        CreateCenterEnrollmentRequest payload = centerEnrollment();
        payload.setEmail(manager.getEmail());
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(classroomOfferingRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));

        assertThatThrownBy(() -> service.createAtCenter(payload, staff.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tài khoản nội bộ");
        verify(classroomOfferingService, never()).enrollStudent(any(), any());
    }

    @Test
    void managerDemandReportSuggestsClassCountFromActivePipeline() {
        when(userRepository.findByEmail(manager.getEmail())).thenReturn(Optional.of(manager));
        when(requestRepository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(
                enrollmentRequest(EnrollmentRequestStatus.SUBMITTED),
                enrollmentRequest(EnrollmentRequestStatus.TEST_SCHEDULED),
                enrollmentRequest(EnrollmentRequestStatus.WAITING_FOR_CLASS)
        ));

        var report = service.getDemandReport(manager.getEmail());

        assertThat(report).hasSize(1);
        assertThat(report.get(0).getTotalRegistrations()).isEqualTo(3);
        assertThat(report.get(0).getSuggestedClassCount()).isEqualTo(1);
    }

    private CompleteEnrollmentTestRequest eligibleResult() {
        CompleteEnrollmentTestRequest payload = new CompleteEnrollmentTestRequest();
        payload.setEligible(true);
        payload.setPlacementLevel(PlacementLevel.INTERMEDIATE);
        payload.setNote("Phù hợp lớp trung cấp.");
        return payload;
    }

    private EnrollmentRequest enrollmentRequest(EnrollmentRequestStatus status) {
        return EnrollmentRequest.builder()
                .id(40L)
                .learner(learner)
                .courseOffering(program)
                .status(status)
                .build();
    }

    private CreateCourseEnrollmentRequest consultationForm() {
        CreateCourseEnrollmentRequest payload = new CreateCourseEnrollmentRequest();
        payload.setCourseOfferingId(program.getId());
        payload.setContactName("Nguyễn Văn Học Viên");
        payload.setContactEmail(learner.getEmail());
        payload.setContactPhone("0901234567");
        payload.setConsultationTrack("IELTS_4_SKILLS");
        payload.setStudyWorkGoal("Mục tiêu IELTS 6.5");
        return payload;
    }

    private CreateCenterEnrollmentRequest centerEnrollment() {
        CreateCenterEnrollmentRequest payload = new CreateCenterEnrollmentRequest();
        payload.setFullName("Nguyễn Học Viên Mới");
        payload.setEmail("new.learner@example.com");
        payload.setPhoneNumber("0901234567");
        payload.setConfirmedLevel(PlacementLevel.INTERMEDIATE);
        payload.setClassroomId(classroom.getId());
        payload.setNote("Đăng ký trực tiếp tại trung tâm.");
        return payload;
    }

    private void stubStaffRequest(EnrollmentRequest request) {
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
    }

    private void stubPersistence() {
        when(historyRepository.findByEnrollmentRequestIdOrderByCreatedAtAscIdAsc(any())).thenReturn(List.of());
        when(requestRepository.save(any(EnrollmentRequest.class))).thenAnswer(invocation -> {
            EnrollmentRequest request = invocation.getArgument(0);
            if (request.getId() == null) request.setId(40L);
            return request;
        });
    }

    private User user(Long id, String email, RoleEnum role) {
        User user = User.builder().id(id).fullName(email).email(email).build();
        user.setRole(role);
        return user;
    }
}
