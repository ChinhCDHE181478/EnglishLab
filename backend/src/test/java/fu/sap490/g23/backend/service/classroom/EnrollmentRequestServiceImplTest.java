package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.AssignEnrollmentClassRequest;
import fu.sap490.g23.backend.dto.request.classroom.CompleteEnrollmentConsultationRequest;
import fu.sap490.g23.backend.dto.request.classroom.CreateCourseEnrollmentRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sap490.g23.backend.dto.response.classroom.CourseEnrollmentRequestResponse;
import fu.sap490.g23.backend.entity.User;
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
import fu.sap490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sap490.g23.backend.repository.classroom.ClassroomOfferingRepository;
import fu.sap490.g23.backend.repository.classroom.EnrollmentRequestRepository;
import fu.sap490.g23.backend.repository.classroom.EnrollmentRequestStatusHistoryRepository;
import fu.sap490.g23.backend.repository.classroom.TrainingProgramRepository;
import fu.sap490.g23.backend.service.assessment.PlacementEligibilityService;
import fu.sap490.g23.backend.service.classroom.impl.EnrollmentRequestServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
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
    @Mock private PlacementTestAttemptRepository placementAttemptRepository;
    @Mock private UserRepository userRepository;
    @Mock private PlacementEligibilityService placementEligibilityService;
    @Mock private ClassroomOfferingService classroomOfferingService;

    private EnrollmentRequestServiceImpl service;
    private User learner;
    private User staff;
    private TrainingProgram program;
    private ClassroomOffering requestedClassroom;

    @BeforeEach
    void setUp() {
        service = new EnrollmentRequestServiceImpl(
                requestRepository,
                historyRepository,
                trainingProgramRepository,
                classroomOfferingRepository,
                placementAttemptRepository,
                userRepository,
                placementEligibilityService,
                classroomOfferingService
        );
        learner = user(10L, "learner@example.com", RoleEnum.LEARNER);
        staff = user(50L, "staff@example.com", RoleEnum.STAFF);
        program = TrainingProgram.builder().id(20L).title("IELTS Foundation")
                .deliveryMode(ClassroomDeliveryMode.OFFLINE).status(PackageStatus.PUBLISHED).maxCapacity(24).build();
        LearningPackage learningPackage = LearningPackage.builder().id(21L).title("IELTS F01")
                .slug("ielts-f01").studyMode("T2, T4, T6 · 18:30").status(PackageStatus.PUBLISHED).build();
        requestedClassroom = ClassroomOffering.builder().id(30L).learningPackage(learningPackage)
                .trainingProgram(program).deliveryMode(ClassroomDeliveryMode.OFFLINE)
                .status(ClassroomOfferingStatus.UPCOMING).startDate(LocalDate.now().plusDays(14)).build();
    }

    @Test
    void submittingGeneralConsultationFormDoesNotRequireClassroomOrPlacement() {
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(requestRepository.existsByLearnerAndStatusNotIn(any(), anySet())).thenReturn(false);
        stubPersistence();
        CreateCourseEnrollmentRequest payload = consultationForm();

        CourseEnrollmentRequestResponse response = service.submit(payload, learner.getEmail());

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.SUBMITTED);
        assertThat(response.getRequestedClassroomId()).isNull();
        assertThat(response.getCourseOfferingId()).isNull();
        assertThat(response.getDesiredClassCode()).isEqualTo("IELTS A01");
        assertThat(response.getContactPhone()).isEqualTo("0901234567");
        assertThat(response.getPlacementAttemptId()).isNull();
        verify(historyRepository).save(any());
    }

    @Test
    void staffCompletesExternalConsultationThenRequestWaitsForClass() {
        EnrollmentRequest request = enrollmentRequest(EnrollmentRequestStatus.SUBMITTED);
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        stubPersistence();
        CompleteEnrollmentConsultationRequest payload = new CompleteEnrollmentConsultationRequest();
        payload.setNote("Đã test tại trung tâm.");

        CourseEnrollmentRequestResponse response = service.completeConsultation(request.getId(), payload, staff.getEmail());

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        assertThat(response.getStaffNote()).isEqualTo("Đã test tại trung tâm.");
    }

    @Test
    void staffAssignsLearnerOnlyAfterExplicitClassSelection() {
        EnrollmentRequest request = enrollmentRequest(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(classroomOfferingRepository.findById(requestedClassroom.getId())).thenReturn(Optional.of(requestedClassroom));
        when(classroomOfferingService.enrollStudent(any(), any())).thenReturn(ClassroomEnrollmentResponse.builder().hasClassAccess(true).build());
        stubPersistence();
        AssignEnrollmentClassRequest payload = new AssignEnrollmentClassRequest();
        payload.setClassroomId(requestedClassroom.getId());

        CourseEnrollmentRequestResponse response = service.assignClass(request.getId(), payload, staff.getEmail());

        assertThat(response.getStatus()).isEqualTo(EnrollmentRequestStatus.CLASS_ASSIGNED);
        assertThat(response.getAssignedClassroomId()).isEqualTo(requestedClassroom.getId());
    }

    @Test
    void staffCannotAssignBeforeConsultationIsCompleted() {
        EnrollmentRequest request = enrollmentRequest(EnrollmentRequestStatus.SUBMITTED);
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        AssignEnrollmentClassRequest payload = new AssignEnrollmentClassRequest();
        payload.setClassroomId(requestedClassroom.getId());

        assertThatThrownBy(() -> service.assignClass(request.getId(), payload, staff.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("hoàn tất tư vấn");
    }

    @Test
    void staffCannotAssignToActiveClass() {
        EnrollmentRequest request = enrollmentRequest(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        requestedClassroom.setStatus(ClassroomOfferingStatus.ACTIVE);
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(classroomOfferingRepository.findById(requestedClassroom.getId())).thenReturn(Optional.of(requestedClassroom));
        AssignEnrollmentClassRequest payload = new AssignEnrollmentClassRequest();
        payload.setClassroomId(requestedClassroom.getId());

        assertThatThrownBy(() -> service.assignClass(request.getId(), payload, staff.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ngày khai giảng trong tương lai");
    }

    @Test
    void staffCannotAssignToUpcomingClassWhoseStartDateHasPassed() {
        EnrollmentRequest request = enrollmentRequest(EnrollmentRequestStatus.WAITING_FOR_CLASS);
        requestedClassroom.setStartDate(LocalDate.now().minusDays(1));
        when(userRepository.findByEmail(staff.getEmail())).thenReturn(Optional.of(staff));
        when(requestRepository.findById(request.getId())).thenReturn(Optional.of(request));
        when(classroomOfferingRepository.findById(requestedClassroom.getId())).thenReturn(Optional.of(requestedClassroom));
        AssignEnrollmentClassRequest payload = new AssignEnrollmentClassRequest();
        payload.setClassroomId(requestedClassroom.getId());

        assertThatThrownBy(() -> service.assignClass(request.getId(), payload, staff.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ngày khai giảng trong tương lai");
    }

    @Test
    void rejectsFormForClassThatIsNotUpcoming() {
        requestedClassroom.setStatus(ClassroomOfferingStatus.CLOSED);
        when(userRepository.findByEmail(learner.getEmail())).thenReturn(Optional.of(learner));
        when(classroomOfferingRepository.findById(requestedClassroom.getId())).thenReturn(Optional.of(requestedClassroom));
        CreateCourseEnrollmentRequest payload = consultationForm();
        payload.setClassroomId(requestedClassroom.getId());

        assertThatThrownBy(() -> service.submit(payload, learner.getEmail()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("không còn mở");
    }

    private EnrollmentRequest enrollmentRequest(EnrollmentRequestStatus status) {
        return EnrollmentRequest.builder().id(40L).learner(learner).courseOffering(program)
                .requestedClassroom(requestedClassroom).status(status).build();
    }

    private CreateCourseEnrollmentRequest consultationForm() {
        CreateCourseEnrollmentRequest payload = new CreateCourseEnrollmentRequest();
        payload.setContactName("Nguyễn Văn Học Viên");
        payload.setContactEmail(learner.getEmail());
        payload.setContactPhone("0901234567");
        payload.setDesiredClassCode("IELTS A01");
        payload.setConsultationTrack("IELTS");
        payload.setStudyWorkGoal("Sinh viên, mục tiêu IELTS 6.5");
        return payload;
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
