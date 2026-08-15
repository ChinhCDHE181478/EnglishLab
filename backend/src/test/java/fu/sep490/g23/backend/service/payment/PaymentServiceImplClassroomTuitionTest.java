package fu.sep490.g23.backend.service.payment;

import fu.sep490.g23.backend.dto.response.payment.PaymentQuoteResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.payment.PaymentOrder;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.LearningPathCourseRepository;
import fu.sep490.g23.backend.repository.course.LearningPathRepository;
import fu.sep490.g23.backend.repository.payment.DiscountCodeRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import fu.sep490.g23.backend.service.payment.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplClassroomTuitionTest {

    @Mock private PayosProperties payosProperties;
    @Mock private PaymentOrderRepository paymentOrderRepository;
    @Mock private DiscountCodeRepository discountCodeRepository;
    @Mock private OnlineCourseRepository onlineCourseRepository;
    @Mock private LearningPathRepository learningPathRepository;
    @Mock private LearningPathCourseRepository learningPathCourseRepository;
    @Mock private ClassroomEnrollmentRepository classroomEnrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private OnlineCourseService onlineCourseService;
    @Mock private ClassroomOfferingService classroomOfferingService;
    @Mock private PaymentReceiptPdfService paymentReceiptPdfService;

    private PaymentServiceImpl paymentService;

    private User student;
    private ClassroomEnrollment enrollment;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                payosProperties,
                paymentOrderRepository,
                discountCodeRepository,
                onlineCourseRepository,
                learningPathRepository,
                learningPathCourseRepository,
                classroomEnrollmentRepository,
                userRepository,
                onlineCourseService,
                classroomOfferingService,
                paymentReceiptPdfService
        );

        student = User.builder().id(7L).email("learner@example.com").fullName("Learner").build();
        LearningPackage learningPackage = LearningPackage.builder().id(3L).title("TOEIC Intensive").price(new BigDecimal("5000000")).build();
        ClassroomOffering offering = ClassroomOffering.builder().id(12L).learningPackage(learningPackage).build();
        enrollment = ClassroomEnrollment.builder()
                .id(88L)
                .student(student)
                .classroomOffering(offering)
                .registrationStatus(ClassroomRegistrationStatus.PENDING_TUITION_PAYMENT)
                .tuitionAmountDue(new BigDecimal("5000000"))
                .tuitionAmountPaid(BigDecimal.ZERO)
                .tuitionDepositPaid(BigDecimal.ZERO)
                .build();
    }

    @Test
    void quotePayment_classroomTuition_returnsRemainingBalance() {
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(student));
        when(classroomEnrollmentRepository.findByStudentIdAndClassroomOfferingId(7L, 12L))
                .thenReturn(Optional.of(enrollment));
        when(paymentOrderRepository.existsByEnrollmentIdAndStatusIn(eq(88L), any())).thenReturn(false);

        PaymentQuoteResponse quote = paymentService.quotePayment(List.of(), List.of(12L), null, "learner@example.com");

        assertEquals(5_000_000L, quote.getTotalAmount());
        assertEquals(5_000_000L, quote.getOriginalAmount());
        assertEquals(0L, quote.getCouponDiscountAmount());
    }

    @Test
    void quotePayment_classroomTuition_rejectsCoupon() {
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(student));
        when(classroomEnrollmentRepository.findByStudentIdAndClassroomOfferingId(7L, 12L))
                .thenReturn(Optional.of(enrollment));
        when(paymentOrderRepository.existsByEnrollmentIdAndStatusIn(eq(88L), any())).thenReturn(false);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> paymentService.quotePayment(List.of(), List.of(12L), "SAVE10", "learner@example.com")
        );
        assertTrue(ex.getMessage().contains("Mã giảm giá"));
    }

    @Test
    void quotePayment_allowsLegacyPendingConfirmation() {
        enrollment.setRegistrationStatus(ClassroomRegistrationStatus.PENDING_CONFIRMATION);
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(student));
        when(classroomEnrollmentRepository.findByStudentIdAndClassroomOfferingId(7L, 12L))
                .thenReturn(Optional.of(enrollment));

        when(paymentOrderRepository.existsByEnrollmentIdAndStatusIn(eq(88L), any())).thenReturn(false);

        PaymentQuoteResponse quote = paymentService.quotePayment(
                List.of(), List.of(12L), null, "learner@example.com"
        );

        assertEquals(5_000_000L, quote.getTotalAmount());
    }

    @Test
    void quotePayment_rejectsWaitlistedEnrollment() {
        enrollment.setRegistrationStatus(ClassroomRegistrationStatus.WAITLIST);
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(student));
        when(classroomEnrollmentRepository.findByStudentIdAndClassroomOfferingId(7L, 12L))
                .thenReturn(Optional.of(enrollment));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> paymentService.quotePayment(List.of(), List.of(12L), null, "learner@example.com")
        );

        assertTrue(ex.getMessage().contains("danh sách chờ"));
    }

    @Test
    void markOrderPaid_appliesClassroomTuition() {
        PaymentOrder order = PaymentOrder.builder()
                .orderCode(123L)
                .student(student)
                .courseIdsCsv("")
                .classroomOfferingIdsCsv("12")
                .enrollmentId(88L)
                .amount(5_000_000L)
                .originalAmount(5_000_000L)
                .systemDiscountAmount(0L)
                .couponDiscountAmount(0L)
                .couponReservationReleased(true)
                .description("ELAB123")
                .status(PaymentOrderStatus.PENDING)
                .build();

        ReflectionTestUtils.invokeMethod(paymentService, "markOrderPaid", order);

        assertEquals(PaymentOrderStatus.PAID, order.getStatus());
        verify(classroomOfferingService).applyPayosTuitionPayment(
                eq(88L),
                eq(new BigDecimal("5000000")),
                eq("PayOS #123")
        );
        verify(onlineCourseService, never()).activatePaidCourse(any(), any());
    }

    @Test
    void createPaymentLink_classroomTuition_zeroAmountShortcut_notUsedWhenBalancePositive() {
        when(userRepository.findByEmail("learner@example.com")).thenReturn(Optional.of(student));
        when(classroomEnrollmentRepository.findByStudentIdAndClassroomOfferingId(7L, 12L))
                .thenReturn(Optional.of(enrollment));
        when(paymentOrderRepository.existsByEnrollmentIdAndStatusIn(eq(88L), any())).thenReturn(false);
        when(payosProperties.isEnabled()).thenReturn(false);

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> paymentService.createPaymentLink(List.of(), List.of(12L), null, "learner@example.com")
        );
        assertTrue(ex.getMessage().contains("PayOS"));
        verify(paymentOrderRepository, never()).save(any());
    }
}
