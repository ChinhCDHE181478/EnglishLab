package fu.sap490.g23.backend.service.classroom;

import fu.sap490.g23.backend.dto.request.classroom.AssignToClassRequest;
import fu.sap490.g23.backend.dto.request.classroom.RegisterClassRequest;
import fu.sap490.g23.backend.dto.request.classroom.RecordTuitionPaymentRequest;
import fu.sap490.g23.backend.dto.request.classroom.ReorderWaitlistRequest;
import fu.sap490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sap490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomOfferingStatus;
import fu.sap490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sap490.g23.backend.entity.classroom.enums.TuitionPaymentKind;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.classroom.*;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.repository.course.PackageTypeRepository;
import fu.sap490.g23.backend.repository.curriculum.CurriculumProgramRepository;
import fu.sap490.g23.backend.security.ClassroomAccessHelper;
import fu.sap490.g23.backend.service.classroom.impl.ClassroomOfferingServiceImpl;
import fu.sap490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sap490.g23.backend.service.notification.ClassroomNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassroomOfferingServiceImplWaitlistTest {

    @Mock private ClassroomOfferingRepository offeringRepository;
    @Mock private ClassroomSessionRepository sessionRepository;
    @Mock private ClassroomEnrollmentRepository enrollmentRepository;
    @Mock private ClassroomTuitionPaymentRepository tuitionPaymentRepository;
    @Mock private ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    @Mock private ClassroomGradebookEntryRepository gradebookEntryRepository;
    @Mock private LearningPackageRepository learningPackageRepository;
    @Mock private PackageTypeRepository packageTypeRepository;
    @Mock private PackageEnrollmentRepository packageEnrollmentRepository;
    @Mock private CurriculumProgramRepository curriculumProgramRepository;
    @Mock private TrainingProgramRepository trainingProgramRepository;
    @Mock private ClassroomMaterialRepository materialRepository;
    @Mock private ClassroomRoomRepository roomRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClassroomMapper mapper;
    @Mock private ClassroomConflictService conflictService;
    @Mock private LarkMeetingService larkMeetingService;
    @Mock private ClassroomAccessHelper accessHelper;
    @Mock private ClassroomNotificationService notificationService;
    @Mock private LarkMeetingParticipantRepository larkParticipantRepository;
    @Mock private CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    @Mock private VirtualAttendanceService virtualAttendanceService;

    @InjectMocks
    private ClassroomOfferingServiceImpl service;

    @Test
    void reorderWaitlist_UpdatesEveryPositionInRequestedOrder() {
        long offeringId = 10L;
        User actor = User.builder().id(99L).email("manager@example.com").build();
        ClassroomOffering offering = ClassroomOffering.builder().id(offeringId).build();
        ClassroomEnrollment first = waitlistedEnrollment(1L, offering, 1);
        ClassroomEnrollment second = waitlistedEnrollment(2L, offering, 2);
        ReorderWaitlistRequest request = new ReorderWaitlistRequest();
        request.setEnrollmentIds(List.of(2L, 1L));

        when(accessHelper.requireUser("manager@example.com")).thenReturn(actor);
        when(offeringRepository.findByIdForUpdate(offeringId)).thenReturn(Optional.of(offering));
        when(enrollmentRepository
                .findByClassroomOfferingIdAndRegistrationStatusOrderByWaitlistPriorityAscEnrolledAtAscIdAsc(
                        offeringId,
                        ClassroomRegistrationStatus.WAITLIST
                ))
                .thenReturn(List.of(first, second));
        when(mapper.toEnrollmentResponse(second))
                .thenReturn(ClassroomEnrollmentResponse.builder().id(2L).waitlistPosition(1).build());
        when(mapper.toEnrollmentResponse(first))
                .thenReturn(ClassroomEnrollmentResponse.builder().id(1L).waitlistPosition(2).build());

        List<ClassroomEnrollmentResponse> result =
                service.reorderWaitlist(offeringId, request, "manager@example.com");

        assertEquals(1, second.getWaitlistPriority());
        assertEquals(2, first.getWaitlistPriority());
        assertEquals(List.of(2L, 1L), result.stream().map(ClassroomEnrollmentResponse::getId).toList());
        verify(enrollmentRepository).saveAll(List.of(first, second));
    }

    @Test
    void reorderWaitlist_RejectsIncompleteQueue() {
        long offeringId = 10L;
        ClassroomOffering offering = ClassroomOffering.builder().id(offeringId).build();
        ReorderWaitlistRequest request = new ReorderWaitlistRequest();
        request.setEnrollmentIds(List.of(1L));

        when(accessHelper.requireUser("manager@example.com")).thenReturn(new User());
        when(offeringRepository.findByIdForUpdate(offeringId)).thenReturn(Optional.of(offering));
        when(enrollmentRepository
                .findByClassroomOfferingIdAndRegistrationStatusOrderByWaitlistPriorityAscEnrolledAtAscIdAsc(
                        offeringId,
                        ClassroomRegistrationStatus.WAITLIST
                ))
                .thenReturn(List.of(
                        waitlistedEnrollment(1L, offering, 1),
                        waitlistedEnrollment(2L, offering, 2)
                ));

        assertThrows(
                RuntimeException.class,
                () -> service.reorderWaitlist(offeringId, request, "manager@example.com")
        );
        verify(enrollmentRepository, never()).saveAll(anyList());
    }

    @Test
    void reorderWaitlist_RejectsUserWithoutTrainingManagerRole() {
        User actor = new User();
        ReorderWaitlistRequest request = new ReorderWaitlistRequest();
        request.setEnrollmentIds(List.of(1L));

        when(accessHelper.requireUser("learner@example.com")).thenReturn(actor);
        doThrow(new RuntimeException("Bạn không có quyền Training Manager."))
                .when(accessHelper)
                .assertTrainingManager(actor);

        assertThrows(
                RuntimeException.class,
                () -> service.reorderWaitlist(10L, request, "learner@example.com")
        );
        verifyNoInteractions(offeringRepository);
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void registerForClass_AppendsLearnerToEndOfWaitlist() {
        long offeringId = 10L;
        User learner = User.builder().id(7L).email("learner@example.com").fullName("Learner").build();
        LearningPackage learningPackage = LearningPackage.builder()
                .id(20L)
                .status(PackageStatus.PUBLISHED)
                .build();
        ClassroomOffering offering = ClassroomOffering.builder()
                .id(offeringId)
                .learningPackage(learningPackage)
                .status(ClassroomOfferingStatus.UPCOMING)
                .startDate(LocalDate.now().plusDays(10))
                .maxCapacity(1)
                .build();
        when(accessHelper.requireUser("learner@example.com")).thenReturn(learner);
        when(offeringRepository.findById(offeringId)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(
                eq(learner.getId()),
                eq(offeringId),
                anyCollection()
        )).thenReturn(false);
        when(sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offeringId))
                .thenReturn(List.of());
        when(enrollmentRepository.countByOfferingAndRegistrationStatuses(eq(offeringId), anyCollection()))
                .thenReturn(1L);
        when(enrollmentRepository.findByStudentIdAndClassroomOfferingId(learner.getId(), offeringId))
                .thenReturn(Optional.empty());
        when(offeringRepository.findByIdForUpdate(offeringId)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.findMaxWaitlistPriority(offeringId, ClassroomRegistrationStatus.WAITLIST))
                .thenReturn(2);
        when(enrollmentRepository.saveAndFlush(any(ClassroomEnrollment.class)))
                .thenAnswer(invocation -> {
                    ClassroomEnrollment saved = invocation.getArgument(0);
                    saved.setId(30L);
                    return saved;
                });
        when(mapper.toEnrollmentResponse(any(ClassroomEnrollment.class)))
                .thenAnswer(invocation -> {
                    ClassroomEnrollment saved = invocation.getArgument(0);
                    return ClassroomEnrollmentResponse.builder()
                            .waitlistPosition(saved.getWaitlistPriority())
                            .build();
                });

        ClassroomEnrollmentResponse response =
                service.registerForClass(offeringId, new RegisterClassRequest(), "learner@example.com");

        assertEquals(3, response.getWaitlistPosition());
        verify(enrollmentRepository).saveAndFlush(argThat(enrollment ->
                enrollment.getRegistrationStatus() == ClassroomRegistrationStatus.WAITLIST
                        && enrollment.getWaitlistPriority() == 3
        ));
        verifyNoInteractions(packageEnrollmentRepository);
        verifyNoInteractions(gradebookEntryRepository);
    }

    @Test
    void registerForClass_WhenSeatAvailable_WaitsForPaymentWithoutLearningAccess() {
        long offeringId = 10L;
        User learner = User.builder().id(7L).email("learner@example.com").fullName("Learner").build();
        LearningPackage learningPackage = LearningPackage.builder()
                .id(20L)
                .status(PackageStatus.PUBLISHED)
                .price(java.math.BigDecimal.valueOf(5_000_000L))
                .build();
        ClassroomOffering offering = ClassroomOffering.builder()
                .id(offeringId)
                .learningPackage(learningPackage)
                .status(ClassroomOfferingStatus.UPCOMING)
                .startDate(LocalDate.now().plusDays(10))
                .maxCapacity(20)
                .build();

        when(accessHelper.requireUser("learner@example.com")).thenReturn(learner);
        when(offeringRepository.findById(offeringId)).thenReturn(Optional.of(offering));
        when(enrollmentRepository.existsByStudentIdAndClassroomOfferingIdAndRegistrationStatusIn(
                eq(learner.getId()), eq(offeringId), anyCollection()
        )).thenReturn(false);
        when(sessionRepository.findByClassroomOfferingIdOrderBySessionDateAscStartTimeAsc(offeringId))
                .thenReturn(List.of());
        when(enrollmentRepository.countByOfferingAndRegistrationStatuses(eq(offeringId), anyCollection()))
                .thenReturn(0L);
        when(enrollmentRepository.findByStudentIdAndClassroomOfferingId(learner.getId(), offeringId))
                .thenReturn(Optional.empty());
        when(enrollmentRepository.saveAndFlush(any(ClassroomEnrollment.class)))
                .thenAnswer(invocation -> {
                    ClassroomEnrollment saved = invocation.getArgument(0);
                    saved.setId(31L);
                    return saved;
                });
        when(mapper.toEnrollmentResponse(any(ClassroomEnrollment.class)))
                .thenAnswer(invocation -> {
                    ClassroomEnrollment saved = invocation.getArgument(0);
                    return ClassroomEnrollmentResponse.builder()
                            .id(saved.getId())
                            .registrationStatus(saved.getRegistrationStatus())
                            .hasClassAccess(saved.hasClassAccess())
                            .build();
                });

        ClassroomEnrollmentResponse response =
                service.registerForClass(offeringId, new RegisterClassRequest(), "learner@example.com");

        assertEquals(ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT, response.getRegistrationStatus());
        assertEquals(false, response.isHasClassAccess());
        verifyNoInteractions(packageEnrollmentRepository);
        verifyNoInteractions(gradebookEntryRepository);
    }

    @Test
    void assignToClass_RejectsEnrollmentBeforeFullPayment() {
        User manager = User.builder().id(99L).email("manager@example.com").build();
        User learner = User.builder().id(7L).email("learner@example.com").build();
        ClassroomOffering offering = ClassroomOffering.builder().id(10L).build();
        ClassroomEnrollment enrollment = ClassroomEnrollment.builder()
                .id(31L)
                .student(learner)
                .classroomOffering(offering)
                .registrationStatus(ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT)
                .build();

        when(accessHelper.requireUser("manager@example.com")).thenReturn(manager);
        when(enrollmentRepository.findById(31L)).thenReturn(Optional.of(enrollment));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.assignToClass(31L, new AssignToClassRequest(), "manager@example.com")
        );

        assertTrue(ex.getMessage().contains("thanh toán"));
        verifyNoInteractions(packageEnrollmentRepository);
        verifyNoInteractions(gradebookEntryRepository);
    }

    @Test
    void recordTuitionPayment_RejectsWaitlistedEnrollment() {
        User manager = User.builder().id(99L).email("manager@example.com").build();
        ClassroomEnrollment enrollment = ClassroomEnrollment.builder()
                .id(32L)
                .classroomOffering(ClassroomOffering.builder().id(10L).build())
                .registrationStatus(ClassroomRegistrationStatus.WAITLIST)
                .build();
        RecordTuitionPaymentRequest request = new RecordTuitionPaymentRequest();
        request.setAmount(java.math.BigDecimal.valueOf(5_000_000L));
        request.setPaymentKind(TuitionPaymentKind.FULL);

        when(accessHelper.requireUser("manager@example.com")).thenReturn(manager);
        when(enrollmentRepository.findById(32L)).thenReturn(Optional.of(enrollment));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> service.recordTuitionPayment(32L, request, "manager@example.com")
        );

        assertTrue(ex.getMessage().contains("danh sách chờ"));
        verifyNoInteractions(tuitionPaymentRepository);
    }

    @Test
    void cancelMyRegistration_CompactsRemainingWaitlist() {
        long offeringId = 10L;
        User learner = User.builder().id(7L).email("learner@example.com").fullName("Learner").build();
        LearningPackage learningPackage = LearningPackage.builder().title("TOEIC").build();
        ClassroomOffering offering = ClassroomOffering.builder()
                .id(offeringId)
                .learningPackage(learningPackage)
                .maxCapacity(1)
                .build();
        ClassroomEnrollment cancelled = waitlistedEnrollment(1L, offering, 1);
        cancelled.setStudent(learner);
        ClassroomEnrollment second = waitlistedEnrollment(2L, offering, 2);
        ClassroomEnrollment third = waitlistedEnrollment(3L, offering, 3);

        when(accessHelper.requireUser("learner@example.com")).thenReturn(learner);
        when(enrollmentRepository.findByStudentIdAndClassroomOfferingId(learner.getId(), offeringId))
                .thenReturn(Optional.of(cancelled));
        when(enrollmentRepository.saveAndFlush(cancelled)).thenReturn(cancelled);
        when(offeringRepository.findByIdForUpdate(offeringId)).thenReturn(Optional.of(offering));
        when(enrollmentRepository
                .findByClassroomOfferingIdAndRegistrationStatusOrderByWaitlistPriorityAscEnrolledAtAscIdAsc(
                        offeringId,
                        ClassroomRegistrationStatus.WAITLIST
                ))
                .thenReturn(List.of(second, third));
        when(enrollmentRepository.countByOfferingAndRegistrationStatuses(eq(offeringId), anyCollection()))
                .thenReturn(1L);
        when(mapper.toEnrollmentResponse(cancelled))
                .thenReturn(ClassroomEnrollmentResponse.builder().id(cancelled.getId()).build());

        service.cancelMyRegistration(offeringId, "learner@example.com");

        assertEquals(1, second.getWaitlistPriority());
        assertEquals(2, third.getWaitlistPriority());
        verify(enrollmentRepository).saveAll(List.of(second, third));
    }

    private ClassroomEnrollment waitlistedEnrollment(
            Long id,
            ClassroomOffering offering,
            Integer priority
    ) {
        return ClassroomEnrollment.builder()
                .id(id)
                .classroomOffering(offering)
                .registrationStatus(ClassroomRegistrationStatus.WAITLIST)
                .waitlistPriority(priority)
                .build();
    }
}
