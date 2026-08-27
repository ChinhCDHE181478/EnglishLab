package fu.sep490.g23.backend.service.payment;

import fu.sep490.g23.backend.dto.request.payment.RefundCourseOrderRequest;
import fu.sep490.g23.backend.dto.response.payment.PaymentOrderSummaryResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.payment.DiscountCode;
import fu.sep490.g23.backend.entity.payment.PaymentOrder;
import fu.sep490.g23.backend.entity.payment.PaymentOrderItem;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderItemType;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.LearningPathCourseRepository;
import fu.sep490.g23.backend.repository.course.LearningPathRepository;
import fu.sep490.g23.backend.repository.payment.DiscountCodeRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderItemRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import fu.sep490.g23.backend.service.payment.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplRefundReceiptTest {

    @Mock private PayosProperties payosProperties;
    @Mock private PaymentOrderRepository paymentOrderRepository;
    @Mock private PaymentOrderItemRepository paymentOrderItemRepository;
    @Mock private DiscountCodeRepository discountCodeRepository;
    @Mock private OnlineCourseRepository onlineCourseRepository;
    @Mock private LearningPathRepository learningPathRepository;
    @Mock private LearningPathCourseRepository learningPathCourseRepository;
    @Mock private ClassEnrollmentRepository classroomEnrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private OnlineCourseService onlineCourseService;
    @Mock private ClassroomOfferingService classroomOfferingService;
    @Mock private PaymentReceiptPdfService paymentReceiptPdfService;

    private PaymentServiceImpl paymentService;
    private User student;
    private User manager;
    private DiscountCode discountCode;
    private PaymentOrder courseOrder;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(
                payosProperties,
                paymentOrderRepository,
                paymentOrderItemRepository,
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

        student = User.builder().id(1L).email("learner@example.com").fullName("Learner").build();
        manager = User.builder().id(2L).email("manager@example.com").fullName("Manager").build();
        discountCode = DiscountCode.builder().id(9L).code("SAVE10").usedCount(3).reservedCount(0).build();
        courseOrder = PaymentOrder.builder()
                .orderCode(1001L)
                .student(student)
                .amount(500_000L)
                .originalAmount(600_000L)
                .systemDiscountAmount(50_000L)
                .couponDiscountAmount(50_000L)
                .discountCode(discountCode)
                .discountCodeText("SAVE10")
                .couponReservationReleased(true)
                .description("ELAB001001")
                .status(PaymentOrderStatus.PAID)
                .build();
        org.mockito.Mockito.lenient().when(paymentOrderItemRepository.findByPaymentOrderIdOrderById(null))
                .thenReturn(List.of(
                        PaymentOrderItem.builder().itemType(PaymentOrderItemType.ONLINE_COURSE)
                                .onlineCourse(OnlineCourse.builder().id(11L).build()).titleSnapshot("Course A").build(),
                        PaymentOrderItem.builder().itemType(PaymentOrderItemType.ONLINE_COURSE)
                                .onlineCourse(OnlineCourse.builder().id(12L).build()).titleSnapshot("Course B").build()
                ));
    }

    @Test
    void refundCourseOrder_revokesAccessAndRestoresCoupon() {
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));
        when(paymentOrderRepository.findByOrderCode(1001L)).thenReturn(Optional.of(courseOrder));

        RefundCourseOrderRequest request = new RefundCourseOrderRequest();
        request.setReason("Học viên yêu cầu hoàn");

        PaymentOrderSummaryResponse response = paymentService.refundCourseOrder(1001L, request, "manager@example.com");

        assertEquals(PaymentOrderStatus.REFUNDED.name(), response.getStatus());
        assertEquals(500_000L, response.getRefundedAmount());
        assertFalse(response.isRefundable());
        assertTrue(response.isHasReceipt());
        assertEquals(2, discountCode.getUsedCount());
        verify(onlineCourseService).revokePaidCourseAccess(11L, "learner@example.com");
        verify(onlineCourseService).revokePaidCourseAccess(12L, "learner@example.com");
        verify(paymentOrderRepository).save(courseOrder);
    }

    @Test
    void refundCourseOrder_rejectsClassroomTuition() {
        when(paymentOrderItemRepository.findByPaymentOrderIdOrderById(null)).thenReturn(List.of(
                PaymentOrderItem.builder().itemType(PaymentOrderItemType.CLASS_ENROLLMENT)
                        .classEnrollment(ClassEnrollment.builder().id(88L).build())
                        .titleSnapshot("Classroom").build()
        ));
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));
        when(paymentOrderRepository.findByOrderCode(1001L)).thenReturn(Optional.of(courseOrder));

        RefundCourseOrderRequest request = new RefundCourseOrderRequest();
        request.setReason("Test");

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> paymentService.refundCourseOrder(1001L, request, "manager@example.com")
        );
        assertTrue(ex.getMessage().contains("học phí lớp"));
    }

    @Test
    void refundCourseOrder_rejectsDoubleRefund() {
        courseOrder.setStatus(PaymentOrderStatus.REFUNDED);
        when(userRepository.findByEmail("manager@example.com")).thenReturn(Optional.of(manager));
        when(paymentOrderRepository.findByOrderCode(1001L)).thenReturn(Optional.of(courseOrder));

        RefundCourseOrderRequest request = new RefundCourseOrderRequest();
        request.setReason("Lần 2");

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> paymentService.refundCourseOrder(1001L, request, "manager@example.com")
        );
        assertTrue(ex.getMessage().contains("đã được hoàn"));
    }

    @Test
    void downloadCourseReceipt_returnsPdfForOwner() {
        byte[] pdf = new byte[]{1, 2, 3};
        when(paymentOrderRepository.findByOrderCode(1001L)).thenReturn(Optional.of(courseOrder));
        when(paymentReceiptPdfService.buildCourseReceipt(courseOrder)).thenReturn(pdf);

        byte[] result = paymentService.downloadCourseReceipt(1001L, "learner@example.com");
        assertArrayEquals(pdf, result);
        verify(paymentReceiptPdfService).buildCourseReceipt(eq(courseOrder));
    }

    @Test
    void downloadCourseReceipt_rejectsOtherStudent() {
        when(paymentOrderRepository.findByOrderCode(1001L)).thenReturn(Optional.of(courseOrder));

        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> paymentService.downloadCourseReceipt(1001L, "other@example.com")
        );
        assertTrue(ex.getMessage().contains("không có quyền"));
    }
}
