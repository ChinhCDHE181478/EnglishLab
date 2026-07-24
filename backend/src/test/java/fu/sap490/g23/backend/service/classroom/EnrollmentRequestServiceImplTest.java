package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.AssignEnrollmentClassRequest;
import fu.sap490.g23.backend.dto.request.classroom.CompleteEnrollmentTestRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateCourseEnrollmentRequest;
import fu.sap490.g23.backend.dto.request.classroom.ScheduleEnrollmentTestRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sap490.g23.backend.dto.response.classroom.CourseEnrollmentRequestResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.EnrollmentRequest;
import fu.sap490.g23.backend.entity.classroom.TrainingProgram;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sap490.g23.backend.entity.classroom.enums.EnrollmentRequestStatus;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.repository.classroom.EnrollmentRequestRepository;
import fu.sap490.g23.backend.repository.classroom.EnrollmentRequestStatusHistoryRepository;
import fu.sap490.g23.backend.repository.classroom.TrainingProgramRepository;
import fu.sap490.g23.backend.service.assessment.PlacementEligibilityService;
import fu.sap490.g23.backend.service.classroom.impl.EnrollmentRequestServiceImpl;
import fu.sap490.g23.backend.service.mail.EnrollmentRequestMailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnrollmentRequestServiceImplTest {
    @Mock private EnrollmentRequestRepository requestRepository;
    @Mock private EnrollmentRequestStatusHistoryRepository historyRepository;
    @Mock private TrainingProgramRepository trainingProgramRepository;
    @Mock private ClassroomOfferingRepository classroomOfferingRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlacementEligibilityService placementEligibilityService;
    @Mock private ClassroomOfferingService classroomOfferingService;
    @Mock private EnrollmentRequestMailService enrollmentRequestMailService;

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
                userRepository,
                placementEligibilityService,
                classroomOfferingService,
                enrollmentRequestMailService
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
        when(requestRepository.existsByLearnerAndStatusNotIn(any(), anySet())).thenReturn(false);
        stubPersistence();

        CourseEnrollmentRequestResponse response = service.submit(consultationForm(), learner.getEmail());

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.SUBMITTED);
        assertThat(response.getCourseOfferingId()).isEqualTo(program.getId());
        assertThat(response.getRequestedClassroomId()).isNull();
        verify(historyRepository).save(any());
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
    void staffCannotRecordResultBeforeAppointment() {
        EnrollmentRequest request = enrollmentRequest(EnrollmentRequestStatus.TEST_SCHEDULED);
        request.setTestAppointmentAt(LocalDateTime.now().plusHours(2));
        stubStaffRequest(request);
        CompleteEnrollmentTestRequest payload = eligibleResult();

        assertThatThrownBy(() -> service.completeTest(request.getId(), payload, staff.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Chưa đến thời gian test");
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
    void staffAssignsOnlyAClassFromRegisteredCourseAndEmailsLearner() {
        EnrollmentRequest request = enrollmentRequest(EnrollmentRequestStatus.WAITING_FOR_CLASS);
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
