package fu.sep490.g23.backend.service.classroom;

import fu.sep490.g23.backend.dto.request.classroom.ResolveTuitionSettlementRequest;
import fu.sep490.g23.backend.dto.response.classroom.ClassroomEnrollmentResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.ClassroomTuitionPayment;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionPaymentKind;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionSettlementStatus;
import fu.sep490.g23.backend.entity.classroom.enums.TuitionSettlementType;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClassroomOfferingServiceImplSettlementTest {

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
    void resolveTuitionSettlement_ApprovesRefundAndWritesAuditPayment() {
        User actor = User.builder().id(30L).email("tm@example.com").fullName("TM").build();
        User student = User.builder().id(27L).email("hv@example.com").fullName("HV").build();
        ClassSection offering = ClassSection.builder().id(12L).name("TOEIC A").build();
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(99L)
                .student(student)
                .classSection(offering)
                .registrationStatus(ClassroomRegistrationStatus.ASSIGNED)
                .tuitionAmountDue(new BigDecimal("1000000"))
                .tuitionAmountPaid(new BigDecimal("1500000"))
                .tuitionSettlementType(TuitionSettlementType.NEED_REFUND)
                .tuitionSettlementStatus(TuitionSettlementStatus.PENDING)
                .tuitionSettlementNote("Cần xử lý hoàn tiền 500000 VND.")
                .build();

        when(accessHelper.requireUser("tm@example.com")).thenReturn(actor);
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);
        when(mapper.toEnrollmentResponse(enrollment))
                .thenReturn(ClassroomEnrollmentResponse.builder().id(99L).tuitionSettlementStatus(TuitionSettlementStatus.RESOLVED).build());

        ResolveTuitionSettlementRequest request = new ResolveTuitionSettlementRequest();
        request.setAction("APPROVE_REFUND");
        request.setNote("Hoàn ngoài hệ thống");

        ClassroomEnrollmentResponse response = service.resolveTuitionSettlement(99L, request, "tm@example.com");

        assertEquals(TuitionSettlementStatus.RESOLVED, response.getTuitionSettlementStatus());
        assertEquals(new BigDecimal("1000000"), enrollment.getTuitionAmountPaid());
        assertEquals(TuitionSettlementType.NONE, enrollment.getTuitionSettlementType());
        assertEquals(TuitionSettlementStatus.RESOLVED, enrollment.getTuitionSettlementStatus());

        ArgumentCaptor<ClassroomTuitionPayment> paymentCaptor = ArgumentCaptor.forClass(ClassroomTuitionPayment.class);
        verify(tuitionPaymentRepository).save(paymentCaptor.capture());
        assertEquals(TuitionPaymentKind.REFUND, paymentCaptor.getValue().getPaymentKind());
        assertEquals(new BigDecimal("500000"), paymentCaptor.getValue().getAmount());
        verify(notificationService).notifyUser(eq(student), eq("CLASSROOM_TUITION_REFUND_APPROVED"), any(), any(), any());
    }

    @Test
    void resolveTuitionSettlement_RejectsWithoutNote() {
        User actor = User.builder().id(30L).email("tm@example.com").build();
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(99L)
                .tuitionSettlementType(TuitionSettlementType.NEED_REFUND)
                .tuitionSettlementStatus(TuitionSettlementStatus.PENDING)
                .build();
        when(accessHelper.requireUser("tm@example.com")).thenReturn(actor);
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.of(enrollment));

        ResolveTuitionSettlementRequest request = new ResolveTuitionSettlementRequest();
        request.setAction("REJECT_REFUND");
        request.setNote("   ");

        assertThrows(RuntimeException.class, () -> service.resolveTuitionSettlement(99L, request, "tm@example.com"));
        verify(tuitionPaymentRepository, never()).save(any());
    }

    @Test
    void resolveTuitionSettlement_RejectsDoubleResolve() {
        User actor = User.builder().id(30L).email("tm@example.com").build();
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(99L)
                .tuitionSettlementType(TuitionSettlementType.NEED_REFUND)
                .tuitionSettlementStatus(TuitionSettlementStatus.RESOLVED)
                .build();
        when(accessHelper.requireUser("tm@example.com")).thenReturn(actor);
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.of(enrollment));

        ResolveTuitionSettlementRequest request = new ResolveTuitionSettlementRequest();
        request.setAction("APPROVE_REFUND");

        assertThrows(RuntimeException.class, () -> service.resolveTuitionSettlement(99L, request, "tm@example.com"));
    }

    @Test
    void resolveTuitionSettlement_ApprovesFullRefundWhenCancelledEvenIfOverpaid() {
        User actor = User.builder().id(30L).email("tm@example.com").fullName("TM").build();
        User student = User.builder().id(27L).email("hv@example.com").fullName("HV").build();
        ClassSection offering = ClassSection.builder().id(12L).name("TOEIC A").build();
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(99L)
                .student(student)
                .classSection(offering)
                .registrationStatus(ClassroomRegistrationStatus.CANCELLED)
                .tuitionAmountDue(new BigDecimal("1000000"))
                .tuitionAmountPaid(new BigDecimal("1200000"))
                .tuitionSettlementType(TuitionSettlementType.NEED_REFUND)
                .tuitionSettlementStatus(TuitionSettlementStatus.PENDING)
                .build();

        when(accessHelper.requireUser("tm@example.com")).thenReturn(actor);
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);
        when(mapper.toEnrollmentResponse(enrollment))
                .thenReturn(ClassroomEnrollmentResponse.builder().id(99L).tuitionSettlementStatus(TuitionSettlementStatus.RESOLVED).build());

        ResolveTuitionSettlementRequest request = new ResolveTuitionSettlementRequest();
        request.setAction("APPROVE_REFUND");
        request.setNote("Hoàn sau khi xóa khỏi lớp");

        service.resolveTuitionSettlement(99L, request, "tm@example.com");

        assertEquals(BigDecimal.ZERO, enrollment.getTuitionAmountPaid());
        ArgumentCaptor<ClassroomTuitionPayment> paymentCaptor = ArgumentCaptor.forClass(ClassroomTuitionPayment.class);
        verify(tuitionPaymentRepository).save(paymentCaptor.capture());
        assertEquals(new BigDecimal("1200000"), paymentCaptor.getValue().getAmount());
        assertEquals(TuitionPaymentKind.REFUND, paymentCaptor.getValue().getPaymentKind());
    }

    @Test
    void resolveTuitionSettlement_RejectsRefundKeepsPaidAmount() {
        User actor = User.builder().id(30L).email("tm@example.com").fullName("TM").build();
        User student = User.builder().id(27L).email("hv@example.com").fullName("HV").build();
        ClassSection offering = ClassSection.builder().id(12L).name("TOEIC A").build();
        ClassEnrollment enrollment = ClassEnrollment.builder()
                .id(99L)
                .student(student)
                .classSection(offering)
                .tuitionAmountDue(new BigDecimal("1000000"))
                .tuitionAmountPaid(new BigDecimal("1500000"))
                .tuitionSettlementType(TuitionSettlementType.NEED_REFUND)
                .tuitionSettlementStatus(TuitionSettlementStatus.PENDING)
                .build();

        when(accessHelper.requireUser("tm@example.com")).thenReturn(actor);
        when(enrollmentRepository.findById(99L)).thenReturn(Optional.of(enrollment));
        when(enrollmentRepository.save(enrollment)).thenReturn(enrollment);
        when(mapper.toEnrollmentResponse(enrollment))
                .thenReturn(ClassroomEnrollmentResponse.builder().id(99L).tuitionSettlementStatus(TuitionSettlementStatus.REJECTED).build());

        ResolveTuitionSettlementRequest request = new ResolveTuitionSettlementRequest();
        request.setAction("REJECT_REFUND");
        request.setNote("Không đủ điều kiện hoàn");

        service.resolveTuitionSettlement(99L, request, "tm@example.com");

        assertEquals(new BigDecimal("1500000"), enrollment.getTuitionAmountPaid());
        assertEquals(TuitionSettlementType.NEED_REFUND, enrollment.getTuitionSettlementType());
        assertEquals(TuitionSettlementStatus.REJECTED, enrollment.getTuitionSettlementStatus());
        assertEquals("Không đủ điều kiện hoàn", enrollment.getTuitionSettlementResolutionNote());
        verify(tuitionPaymentRepository, never()).save(any());
        verify(notificationService).notifyUser(eq(student), eq("CLASSROOM_TUITION_REFUND_REJECTED"), any(), any(), any());
    }
}
