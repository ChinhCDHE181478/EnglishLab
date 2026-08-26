package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.AssignToClassRequest;
import fu.sep490.g23.backend.dto.request.classroom.RecordTuitionPaymentRequest;
import fu.sep490.g23.backend.dto.request.classroom.ReorderWaitlistRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomOfferingResponse;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomSessionResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassSchedule;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomDeliveryMode;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomSessionStatus;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionPaymentKind;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.*;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.security.ClassroomAccessHelper;
import fu.sep490.g23.backend.service.classroom.impl.ClassroomOfferingServiceImpl;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sep490.g23.backend.service.notification.ClassroomNotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClassroomOfferingServiceImplWaitlistTest {

    @Mock private ClassSectionRepository offeringRepository;
    @Mock private ClassScheduleRepository sessionRepository;
    @Mock private ClassEnrollmentRepository enrollmentRepository;
    @Mock private ClassroomTuitionPaymentRepository tuitionPaymentRepository;
    @Mock private ClassroomTeacherAssignmentRepository teacherAssignmentRepository;
    @Mock private ClassroomGradebookEntryRepository gradebookEntryRepository;
    @Mock private OnlineCourseEnrollmentRepository packageEnrollmentRepository;
    @Mock private InstructorLedCourseRepository instructorLedCourseRepository;
    @Mock private ClassroomMaterialRepository materialRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private UserRepository userRepository;
    @Mock private ClassroomMapper mapper;
    @Mock private ClassroomConflictService conflictService;
    @Mock private VirtualMeetingService virtualMeetingService;
    @Mock private ClassroomAccessHelper accessHelper;
    @Mock private ClassroomNotificationService notificationService;
    @Mock private CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    @Mock private VirtualAttendanceService virtualAttendanceService;

    @InjectMocks
    private ClassroomOfferingServiceImpl service;

    @Test
    void getMyClasses_ReturnsOnlyStaffAssignedClassrooms() {
        long learnerId = 7L;
        long offeringId = 10L;
        User learner = User.builder().id(learnerId).email("learner@example.com").build();
        ClassSection offering = ClassSection.builder()
                .id(offeringId)
                .name("Offering 20")
                .build();
        ClassEnrollment assigned = ClassEnrollment.builder()
                .id(30L)
                .student(learner)
                .classSection(offering)
                .registrationStatus(ClassroomRegistrationStatus.ASSIGNED)
                .build();
        ClassroomOfferingResponse mapped = ClassroomOfferingResponse.builder()
                .id(offeringId)
                .hasClassAccess(true)
                .build();

        when(accessHelper.requireUser(learner.getEmail())).thenReturn(learner);
        when(enrollmentRepository.findByStudentIdAndRegistrationStatusIn(
                learnerId,
                ClassroomRegistrationSupport.HAS_LEARNING_ACCESS
        )).thenReturn(List.of(assigned));
        when(enrollmentRepository.findByStudentIdAndClassSectionId(learnerId, offeringId))
                .thenReturn(Optional.of(assigned));
        when(mapper.toOfferingResponse(offering, false, learnerId, assigned, false)).thenReturn(mapped);

        List<ClassroomOfferingResponse> result = service.getMyClasses(learner.getEmail());

        assertEquals(List.of(mapped), result);
        verify(enrollmentRepository).findByStudentIdAndRegistrationStatusIn(
                learnerId,
                ClassroomRegistrationSupport.HAS_LEARNING_ACCESS
        );
    }

    @Test
    void getLearnerOffering_RejectsLegacyPendingEnrollment() {
        long learnerId = 7L;
        long offeringId = 10L;
        User learner = User.builder().id(learnerId).email("learner@example.com").build();
        ClassEnrollment pending = ClassEnrollment.builder()
                .student(learner)
                .classSection(ClassSection.builder().id(offeringId).build())
                .registrationStatus(ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT)
                .build();

        when(accessHelper.requireUser(learner.getEmail())).thenReturn(learner);
        when(enrollmentRepository.findByStudentIdAndClassSectionId(learnerId, offeringId))
                .thenReturn(Optional.of(pending));

        RuntimeException error = assertThrows(
                RuntimeException.class,
                () -> service.getLearnerOffering(offeringId, learner.getEmail())
        );

        assertTrue(error.getMessage().contains("không có quyền"));
        verify(offeringRepository, never()).findById(anyLong());
    }

    @Test
    void getLearnerOffering_IncludesAssignedEnrollmentAccess() {
        long learnerId = 7L;
        long offeringId = 10L;
        User learner = User.builder().id(learnerId).email("learner@example.com").build();
        ClassSection offering = ClassSection.builder()
                .id(offeringId)
                .name("Offering 20")
                .build();
        ClassEnrollment assigned = ClassEnrollment.builder()
                .id(30L)
                .student(learner)
                .classSection(offering)
                .registrationStatus(ClassroomRegistrationStatus.ASSIGNED)
                .build();
        ClassroomOfferingResponse mapped = ClassroomOfferingResponse.builder()
                .id(offeringId)
                .enrollmentId(assigned.getId())
                .hasClassAccess(true)
                .registrationStatus(ClassroomRegistrationStatus.ASSIGNED)
                .build();

        when(accessHelper.requireUser(learner.getEmail())).thenReturn(learner);
        when(enrollmentRepository.findByStudentIdAndClassSectionId(learnerId, offeringId))
                .thenReturn(Optional.of(assigned));
        when(offeringRepository.findById(offeringId)).thenReturn(Optional.of(offering));
        when(mapper.toOfferingResponse(offering, true, learnerId, assigned, true)).thenReturn(mapped);

        ClassroomOfferingResponse result = service.getLearnerOffering(offeringId, learner.getEmail());

        assertEquals(assigned.getId(), result.getEnrollmentId());
        assertTrue(result.isHasClassAccess());
        assertEquals(ClassroomRegistrationStatus.ASSIGNED, result.getRegistrationStatus());
    }

    @Test
    void reorderWaitlist_UpdatesEveryPositionInRequestedOrder() {
        long offeringId = 10L;
        User actor = User.builder().id(99L).email("manager@example.com").build();
        ClassSection offering = ClassSection.builder().id(offeringId).build();
        ClassEnrollment first = waitlistedEnrollment(1L, offering, 1);
        ClassEnrollment second = waitlistedEnrollment(2L, offering, 2);
        ReorderWaitlistRequest request = new ReorderWaitlistRequest();
        request.setEnrollmentIds(List.of(2L, 1L));

        when(accessHelper.requireUser("manager@example.com")).thenReturn(actor);
        when(offeringRepository.findByIdForUpdate(offeringId)).thenReturn(Optional.of(offering));
        when(enrollmentRepository
                .findByClassSectionIdAndRegistrationStatusOrderByWaitlistPriorityAscEnrolledAtAscIdAsc(
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
        ClassSection offering = ClassSection.builder().id(offeringId).build();
        ReorderWaitlistRequest request = new ReorderWaitlistRequest();
        request.setEnrollmentIds(List.of(1L));

        when(accessHelper.requireUser("manager@example.com")).thenReturn(new User());
        when(offeringRepository.findByIdForUpdate(offeringId)).thenReturn(Optional.of(offering));
        when(enrollmentRepository
                .findByClassSectionIdAndRegistrationStatusOrderByWaitlistPriorityAscEnrolledAtAscIdAsc(
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
    void reorderWaitlistRejectsUserWithoutStaffOperatorRole() {
        User actor = new User();
        ReorderWaitlistRequest request = new ReorderWaitlistRequest();
        request.setEnrollmentIds(List.of(1L));

        when(accessHelper.requireUser("learner@example.com")).thenReturn(actor);
        doThrow(new RuntimeException("Bạn không có quyền vận hành đào tạo."))
                .when(accessHelper)
                .assertStaffOperator(actor);

        assertThrows(
                RuntimeException.class,
                () -> service.reorderWaitlist(10L, request, "learner@example.com")
        );
        verifyNoInteractions(offeringRepository);
        verifyNoInteractions(enrollmentRepository);
    }

    @Test
    void assignToClass_RejectsEnrollmentBeforeFullPayment() {
        User manager = User.builder().id(99L).email("manager@example.com").build();
        User learner = User.builder().id(7L).email("learner@example.com").build();
        ClassSection offering = ClassSection.builder().id(10L).build();
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(31L)
                .student(learner)
                .classSection(offering)
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
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(32L)
                .classSection(ClassSection.builder().id(10L).build())
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

    private ClassEnrollment waitlistedEnrollment(
            Long id,
            ClassSection offering,
            Integer priority
    ) {
        return ClassEnrollment.builder()
                .id(id)
                .classSection(offering)
                .registrationStatus(ClassroomRegistrationStatus.WAITLIST)
                .waitlistPriority(priority)
                .build();
    }
}
