package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.AssignEnrollmentClassRequest;
import fu.sep490.g23.backend.dto.request.classroom.CompleteEnrollmentTestRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateCenterEnrollmentRequest;
import fu.sep490.g23.backend.dto.request.classroom.CreateCourseEnrollmentRequest;
import fu.sep490.g23.backend.dto.request.classroom.ScheduleEnrollmentTestRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sep490.g23.backend.dto.response.classroom.ConflictCheckResultResponse;
import fu.sep490.g23.backend.dto.response.classroom.CourseEnrollmentRequestResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.AuthToken;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.CourseRegistrationRequest;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.classroom.ClassSectionRepository;
import fu.sep490.g23.backend.repository.classroom.ClassScheduleRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.classroom.CourseRegistrationRequestRepository;
import fu.sep490.g23.backend.repository.classroom.EnrollmentRequestStatusHistoryRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.service.assessment.PlacementEligibilityService;
import fu.sep490.g23.backend.service.assessment.PlacementTestDefinitionService;
import fu.sep490.g23.backend.service.auth.AuthTokenService;
import fu.sep490.g23.backend.service.classroom.impl.EnrollmentRequestServiceImpl;
import fu.sep490.g23.backend.service.mail.EnrollmentRequestMailService;
import fu.sep490.g23.backend.service.mail.AuthMailService;
import fu.sep490.g23.backend.service.user.UserRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
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
    @Mock private CourseRegistrationRequestRepository requestRepository;
    @Mock private EnrollmentRequestStatusHistoryRepository historyRepository;
    @Mock private InstructorLedCourseRepository instructorLedCourseRepository;
    @Mock private ClassSectionRepository classroomOfferingRepository;
    @Mock private ClassScheduleRepository classroomSessionRepository;
    @Mock private ClassEnrollmentRepository classroomEnrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlacementTestAttemptRepository placementTestAttemptRepository;
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
    private User secondStaff;
    private User manager;
    private InstructorLedCourse program;
    private ClassSection classroom;

    @BeforeEach
    void setUp() {
        service = new EnrollmentRequestServiceImpl(
                requestRepository,
                historyRepository,
                instructorLedCourseRepository,
                classroomOfferingRepository,
                classroomSessionRepository,
                classroomEnrollmentRepository,
                userRepository,
                placementTestAttemptRepository,
                placementEligibilityService,
                classroomOfferingService,
                classroomConflictService,
                enrollmentRequestMailService,
                authTokenService,
                authMailService,
                userRoleService,
                passwordEncoder
        );
        learner = user(10L, "learner@example.com", RoleCodes.LEARNER);
        staff = user(50L, "staff@example.com", RoleCodes.STAFF);
        secondStaff = user(51L, "staff2@example.com", RoleCodes.STAFF);
        manager = user(60L, "manager@example.com", RoleCodes.MANAGER);
        program = InstructorLedCourse.builder()
                .id(20L)
                .code("IELTS-FOUNDATION")
                .title("IELTS Foundation")
                .publicationStatus(PackageStatus.PUBLISHED)
                .build();
        classroom = ClassSection.builder()
                .id(30L)
                .name("IELTS F01")
                .code("ielts-f01")
                .instructorLedCourse(InstructorLedCourse.builder()
                        .id(program.getId())
                        .code(program.getCode())
                        .title(program.getTitle())
                        .publicationStatus(PackageStatus.PUBLISHED)
                        .build())
                .deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .status(ClassroomOfferingStatus.UPCOMING)
                .startDate(LocalDate.now().plusDays(14))
                .build();
    }

    @Test
    void learnerRegistersInterestInCourseInsteadOfClass() {
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(instructorLedCourseRepository.findById(program.getId())).thenReturn(Optional.of(program));
        when(requestRepository.existsByLearnerAndCourseOfferingAndStatusNotIn(any(), any(), anySet()))
                .thenReturn(false);
        when(userRepository.findEnabledByRoleCodeForUpdate(RoleCodes.STAFF))
                .thenReturn(List.of(staff, secondStaff));
        when(requestRepository.findFirstByReviewedByInAndReviewedAtIsNotNullAndRequestSourceOrderByReviewedAtDescIdDesc(
                List.of(staff, secondStaff),
                fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestSource.ONLINE
        ))
                .thenReturn(Optional.empty());
        stubPersistence();

        CourseEnrollmentRequestResponse response = service.submit(consultationForm(), learner.getEmail());

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.SUBMITTED);
        assertThat(response.getCourseOfferingId()).isEqualTo(program.getId());
        assertThat(response.getRequestedClassroomId()).isNull();
        ArgumentCaptor<CourseRegistrationRequest> requestCaptor = ArgumentCaptor.forClass(CourseRegistrationRequest.class);
        verify(requestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getReviewedBy()).isEqualTo(staff);
        verify(historyRepository).save(any());
        verify(requestRepository).existsByLearnerAndCourseOfferingAndStatusNotIn(
                eq(learner),
                eq(program),
                anySet()
        );
    }

    @Test
    void learnerRegistrationMovesToNextStaffInRoundRobinOrder() {
        CourseRegistrationRequest previous = courseRegistrationRequest(EnrollmentRequestStatus.CLASS_ASSIGNED);
        previous.setReviewedBy(staff);
        previous.setReviewedAt(LocalDateTime.now().minusMinutes(1));
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(instructorLedCourseRepository.findById(program.getId())).thenReturn(Optional.of(program));
        when(requestRepository.existsByLearnerAndCourseOfferingAndStatusNotIn(any(), any(), anySet()))
                .thenReturn(false);
        when(userRepository.findEnabledByRoleCodeForUpdate(RoleCodes.STAFF))
                .thenReturn(List.of(staff, secondStaff));
        when(requestRepository.findFirstByReviewedByInAndReviewedAtIsNotNullAndRequestSourceOrderByReviewedAtDescIdDesc(
                List.of(staff, secondStaff),
                fu.sep490.g23.backend.entity.classroom.enums.EnrollmentRequestSource.ONLINE
        ))
                .thenReturn(Optional.of(previous));
        stubPersistence();

        service.submit(consultationForm(), learner.getEmail());

        ArgumentCaptor<CourseRegistrationRequest> requestCaptor = ArgumentCaptor.forClass(CourseRegistrationRequest.class);
        verify(requestRepository).save(requestCaptor.capture());
        assertThat(requestCaptor.getValue().getReviewedBy()).isEqualTo(secondStaff);
    }

    @Test
    void staffOnlyListsAssignedRequests() {
        CourseRegistrationRequest assigned = courseRegistrationRequest(EnrollmentRequestStatus.SUBMITTED);
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(requestRepository.findByReviewedByOrderByCreatedAtDesc(staff)).thenReturn(List.of(assigned));
        when(historyRepository.findByCourseRegistrationRequestIdOrderByCreatedAtAscIdAsc(assigned.getId()))
                .thenReturn(List.of());

        List<CourseEnrollmentRequestResponse> responses = service.listForStaff(null, staff.getEmail());

        assertThat(responses).extracting(CourseEnrollmentRequestResponse::getId).containsExactly(assigned.getId());
        verify(requestRepository, never()).findAllByOrderByCreatedAtDesc();
    }

    @Test
    void staffSeesLatestFullPlacementResultAsReferenceWithoutChangingRequestStatus() {
        CourseRegistrationRequest request = courseRegistrationRequest(EnrollmentRequestStatus.TEST_SCHEDULED);
        PlacementTestAttempt skillAssessment = PlacementTestAttempt.builder()
                .id(72L)
                .student(learner)
                .testCode(PlacementTestDefinitionService.TEST_CODE)
                .aiFeedbackJson("{\"examType\":\"SKILL\"}")
                .overallScore(new BigDecimal("7.0"))
                .evaluationStatus(PlacementEvaluationStatus.NOT_ELIGIBLE)
                .submittedAt(LocalDateTime.now())
                .build();
        PlacementTestAttempt fullPlacement = PlacementTestAttempt.builder()
                .id(71L)
                .student(learner)
                .testCode(PlacementTestDefinitionService.TEST_CODE)
                .aiFeedbackJson("{\"examType\":\"IELTS\"}")
                .listeningScore(new BigDecimal("6.5"))
                .readingScore(new BigDecimal("6.0"))
                .writingScore(new BigDecimal("5.5"))
                .speakingScore(new BigDecimal("6.0"))
                .overallScore(new BigDecimal("6.0"))
                .evaluationStatus(PlacementEvaluationStatus.ELIGIBLE)
                .recommendedLevel(PlacementLevel.INTERMEDIATE)
                .submittedAt(LocalDateTime.now().minusDays(1))
                .build();
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(requestRepository.findByReviewedByOrderByCreatedAtDesc(staff)).thenReturn(List.of(request));
        when(historyRepository.findByCourseRegistrationRequestIdOrderByCreatedAtAscIdAsc(request.getId()))
                .thenReturn(List.of());
        when(placementTestAttemptRepository.findByStudentAndTestCodeOrderBySubmittedAtDesc(
                learner,
                PlacementTestDefinitionService.TEST_CODE
        )).thenReturn(List.of(skillAssessment, fullPlacement));

        CourseEnrollmentRequestResponse response = service.listForStaff(null, staff.getEmail()).get(0);

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.TEST_SCHEDULED);
        assertThat(response.getLatestPlacementResult()).isNotNull();
        assertThat(response.getLatestPlacementResult().getAttemptId()).isEqualTo(fullPlacement.getId());
        assertThat(response.getLatestPlacementResult().getExamType()).isEqualTo("IELTS");
        assertThat(response.getLatestPlacementResult().getOverallScore()).isEqualByComparingTo("6.0");
        assertThat(request.getPlacementAttempt()).isNull();
    }

    @Test
    void staffCannotProcessRequestAssignedToAnotherStaff() {
        CourseRegistrationRequest request = courseRegistrationRequest(EnrollmentRequestStatus.SUBMITTED);
        request.setReviewedBy(secondStaff);
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(requestRepository.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        ScheduleEnrollmentTestRequest payload = new ScheduleEnrollmentTestRequest();
        payload.setAppointmentAt(LocalDateTime.now().plusDays(1));
        payload.setLocation("EnglishLab Campus");

        assertThatThrownBy(() -> service.scheduleTest(request.getId(), payload, staff.getEmail()))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class)
                .hasMessageContaining("nhân viên khác");

        verify(enrollmentRequestMailService, never()).sendTestAppointment(any());
    }

    @Test
    void learnerCannotCreateSecondActiveRequestForSameCourse() {
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(instructorLedCourseRepository.findById(program.getId())).thenReturn(Optional.of(program));
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
        CourseRegistrationRequest request = courseRegistrationRequest(EnrollmentRequestStatus.SUBMITTED);
        stubStaffRequest(request);
        stubPersistence();
        ScheduleEnrollmentTestRequest payload = new ScheduleEnrollmentTestRequest();
        payload.setAppointmentAt(LocalDateTime.now().plusDays(2).withSecond(0).withNano(0));
        payload.setLocation("EnglishLab Campus");

        CourseEnrollmentRequestResponse response = service.scheduleTest(request.getId(), payload, staff.getEmail());

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.TEST_SCHEDULED);
        assertThat(response.getInvitationSentAt()).isNotNull();
        verify(enrollmentRequestMailService).sendTestAppointment(request);
    }

    @Test
    void staffCanRecordResultWhenLearnerArrivesBeforeAppointment() {
        CourseRegistrationRequest request = courseRegistrationRequest(EnrollmentRequestStatus.TEST_SCHEDULED);
        stubStaffRequest(request);
        stubPersistence();

        CourseEnrollmentRequestResponse response = service.completeTest(
                request.getId(),
                eligibleResult(),
                staff.getEmail()
        );

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        verify(enrollmentRequestMailService).sendTestResult(request, true, program.getTitle());
    }

    @Test
    void passedTestMovesLearnerToWaitingForClass() {
        CourseRegistrationRequest request = courseRegistrationRequest(EnrollmentRequestStatus.TEST_SCHEDULED);
        stubStaffRequest(request);
        stubPersistence();

        CourseEnrollmentRequestResponse response = service.completeTest(request.getId(), eligibleResult(), staff.getEmail());

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        assertThat(response.getConfirmedLevel()).isNull();
    }

    @Test
    void failedTestWithAlternativeCourseWaitsForLearnerConfirmationAndEmailsResult() {
        CourseRegistrationRequest request = courseRegistrationRequest(EnrollmentRequestStatus.TEST_SCHEDULED);
        InstructorLedCourse recommendation = InstructorLedCourse.builder()
                .id(21L)
                .code("IELTS-STARTER")
                .title("IELTS Starter")
                .publicationStatus(PackageStatus.PUBLISHED)
                .build();
        stubStaffRequest(request);
        when(instructorLedCourseRepository.findById(recommendation.getId())).thenReturn(Optional.of(recommendation));
        stubPersistence();
        CompleteEnrollmentTestRequest payload = new CompleteEnrollmentTestRequest();
        payload.setEligible(false);
        payload.setRecommendedCourseOfferingId(recommendation.getId());
        payload.setNote("Nên củng cố nền tảng trước.");

        CourseEnrollmentRequestResponse response = service.completeTest(request.getId(), payload, staff.getEmail());

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.CLASS_PROPOSED);
        assertThat(response.getCourseOfferingId()).isEqualTo(recommendation.getId());
        verify(enrollmentRequestMailService).sendTestResult(request, false, program.getTitle());
    }

    @Test
    void failedTestWithoutAlternativeCourseEndsRequestAndEmailsResult() {
        CourseRegistrationRequest request = courseRegistrationRequest(EnrollmentRequestStatus.TEST_SCHEDULED);
        stubStaffRequest(request);
        stubPersistence();
        CompleteEnrollmentTestRequest payload = new CompleteEnrollmentTestRequest();
        payload.setEligible(false);
        payload.setNote("Chưa đáp ứng đầu vào tối thiểu.");

        CourseEnrollmentRequestResponse response = service.completeTest(request.getId(), payload, staff.getEmail());

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.REJECTED);
        verify(enrollmentRequestMailService).sendTestResult(request, false, program.getTitle());
    }

    @Test
    void learnerAcceptsRecommendedCourseBeforeWaitingForClass() {
        CourseRegistrationRequest request = courseRegistrationRequest(EnrollmentRequestStatus.CLASS_PROPOSED);
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(requestRepository.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        stubPersistence();

        CourseEnrollmentRequestResponse response = service.respondToCourseRecommendation(
                request.getId(),
                true,
                learner.getEmail()
        );

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.WAITING_FOR_CLASS);
    }

    @Test
    void learnerDeclinesRecommendedCourseAndClosesRequest() {
        CourseRegistrationRequest request = courseRegistrationRequest(EnrollmentRequestStatus.CLASS_PROPOSED);
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(requestRepository.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
        stubPersistence();

        CourseEnrollmentRequestResponse response = service.respondToCourseRecommendation(
                request.getId(),
                false,
                learner.getEmail()
        );

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.CANCELLED);
        assertThat(response.getRejectionReason()).contains("không đồng ý");
    }

    @Test
    void staffCanAssignClassFromConfirmedCourseAndEmailsLearner() {
        CourseRegistrationRequest request = courseRegistrationRequest(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        stubStaffRequest(request);
        when(classroomOfferingRepository.findByIdForUpdate(classroom.getId())).thenReturn(Optional.of(classroom));
        when(userRepository.findByIdForUpdate(learner.getId())).thenReturn(Optional.of(learner));
        when(classroomConflictService.check(any()))
                .thenReturn(ConflictCheckResultResponse.builder().build());
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
    void staffCannotAssignClassFromAnotherCourse() {
        CourseRegistrationRequest request = courseRegistrationRequest(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        classroom.setInstructorLedCourse(InstructorLedCourse.builder()
                .id(99L)
                .title("Khóa học khác")
                .publicationStatus(PackageStatus.PUBLISHED)
                .build());
        stubStaffRequest(request);
        when(classroomOfferingRepository.findByIdForUpdate(classroom.getId())).thenReturn(Optional.of(classroom));
        AssignEnrollmentClassRequest payload = new AssignEnrollmentClassRequest();
        payload.setClassroomId(classroom.getId());

        assertThatThrownBy(() -> service.assignClass(request.getId(), payload, staff.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("khóa học đã được học viên xác nhận");

        verify(classroomOfferingService, never()).enrollStudent(any(), any());
    }

    @Test
    void staffCannotAssignClassWhenLearnerScheduleConflictsAtConfirmationTime() {
        CourseRegistrationRequest request = courseRegistrationRequest(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        stubStaffRequest(request);
        when(classroomOfferingRepository.findByIdForUpdate(classroom.getId())).thenReturn(Optional.of(classroom));
        when(userRepository.findByIdForUpdate(learner.getId())).thenReturn(Optional.of(learner));
        when(classroomConflictService.check(any()))
                .thenReturn(ConflictCheckResultResponse.builder().hasBlockingConflict(true).build());
        AssignEnrollmentClassRequest payload = new AssignEnrollmentClassRequest();
        payload.setClassroomId(classroom.getId());

        assertThatThrownBy(() -> service.assignClass(request.getId(), payload, staff.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("lịch học bị trùng");

        verify(classroomOfferingService, never()).enrollStudent(any(), any());
    }

    @Test
    void staffCannotAssignBeforeTestIsCompleted() {
        CourseRegistrationRequest request = courseRegistrationRequest(EnrollmentRequestStatus.TEST_SCHEDULED);
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
            saved.setRoles(fu.sep490.g23.backend.support.TestRoles.roles(RoleCodes.LEARNER));
            return saved;
        });
        when(classroomEnrollmentRepository
                .existsByStudentIdAndClassSectionIdAndRegistrationStatusIn(any(), any(), anySet()))
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
        verify(userRoleService).assignRole(any(User.class), eq(RoleCodes.LEARNER));
        verify(authMailService).sendStaffCreatedAccountEmail(any(User.class), eq("123456"));
        verify(enrollmentRequestMailService).sendClassAssignment(any(CourseRegistrationRequest.class), eq(classroom));
    }

    @Test
    void staffCannotCreateDuplicateCenterEnrollmentInSameClass() {
        CreateCenterEnrollmentRequest payload = centerEnrollment();
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(classroomOfferingRepository.findById(classroom.getId())).thenReturn(Optional.of(classroom));
        when(userRepository.findByEmail("new.learner@example.com")).thenReturn(Optional.of(learner));
        when(userRepository.save(learner)).thenReturn(learner);
        when(classroomEnrollmentRepository
                .existsByStudentIdAndClassSectionIdAndRegistrationStatusIn(any(), any(), anySet()))
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
                .existsByStudentIdAndClassSectionIdAndRegistrationStatusIn(any(), any(), anySet()))
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
                courseRegistrationRequest(EnrollmentRequestStatus.SUBMITTED),
                courseRegistrationRequest(EnrollmentRequestStatus.TEST_SCHEDULED),
                courseRegistrationRequest(EnrollmentRequestStatus.WAITING_FOR_CLASS)
        ));

        var report = service.getDemandReport(manager.getEmail());

        assertThat(report).hasSize(1);
        assertThat(report.get(0).getTotalRegistrations()).isEqualTo(3);
        assertThat(report.get(0).getSuggestedClassCount()).isEqualTo(1);
    }

    private CompleteEnrollmentTestRequest eligibleResult() {
        CompleteEnrollmentTestRequest payload = new CompleteEnrollmentTestRequest();
        payload.setEligible(true);
        payload.setNote("Phù hợp lớp trung cấp.");
        return payload;
    }

    private CourseRegistrationRequest courseRegistrationRequest(EnrollmentRequestStatus status) {
        return CourseRegistrationRequest.builder()
                .id(40L)
                .learner(learner)
                .courseOffering(program)
                .status(status)
                .reviewedBy(staff)
                .reviewedAt(LocalDateTime.now().minusDays(1))
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

    private void stubStaffRequest(CourseRegistrationRequest request) {
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(requestRepository.findByIdForUpdate(request.getId())).thenReturn(Optional.of(request));
    }

    private void stubPersistence() {
        when(historyRepository.findByCourseRegistrationRequestIdOrderByCreatedAtAscIdAsc(any())).thenReturn(List.of());
        when(requestRepository.save(any(CourseRegistrationRequest.class))).thenAnswer(invocation -> {
            CourseRegistrationRequest request = invocation.getArgument(0);
            if (request.getId() == null) request.setId(40L);
            return request;
        });
    }

    private User user(Long id, String email, String roleCode) {
        User user = User.builder().id(id).fullName(email).email(email).build();
        user.setRoles(fu.sep490.g23.backend.support.TestRoles.roles(roleCode));
        return user;
    }
}
