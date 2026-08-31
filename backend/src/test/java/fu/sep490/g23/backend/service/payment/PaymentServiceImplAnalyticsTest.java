package fu.sep490.g23.backend.service.payment;

import fu.sep490.g23.backend.dto.response.payment.RevenueAnalyticsResponse;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.LearningPathCourseRepository;
import fu.sep490.g23.backend.repository.course.LearningPathRepository;
import fu.sep490.g23.backend.repository.payment.DiscountCodeRepository;
import fu.sep490.g23.backend.repository.payment.PaymentMonthlyRevenueProjection;
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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplAnalyticsTest {

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
    @Mock private PaymentMonthlyRevenueProjection monthlyRevenue;

    private PaymentServiceImpl paymentService;

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
    }

    @Test
    void getRevenueAnalytics_usesDatabaseAggregatesInsteadOfFindAll() {
        when(paymentOrderRepository.count()).thenReturn(100L);
        when(paymentOrderRepository.countByStatus(PaymentOrderStatus.PAID)).thenReturn(70L);
        when(paymentOrderRepository.countByStatusIn(List.of(
                PaymentOrderStatus.FAILED,
                PaymentOrderStatus.CANCELLED,
                PaymentOrderStatus.EXPIRED,
                PaymentOrderStatus.REFUNDED
        ))).thenReturn(20L);
        when(paymentOrderRepository.countByStatusIn(List.of(
                PaymentOrderStatus.PENDING,
                PaymentOrderStatus.PROCESSING
        ))).thenReturn(10L);
        when(paymentOrderRepository.sumAmountByStatus(PaymentOrderStatus.PAID)).thenReturn(5_000_000L);
        when(paymentOrderRepository.sumDiscountByStatus(PaymentOrderStatus.PAID)).thenReturn(500_000L);
        when(paymentOrderRepository.sumCouponDiscountByStatus(PaymentOrderStatus.PAID)).thenReturn(200_000L);
        when(monthlyRevenue.getYearValue()).thenReturn(2026);
        when(monthlyRevenue.getMonthValue()).thenReturn(8);
        when(monthlyRevenue.getRevenueVnd()).thenReturn(5_000_000L);
        when(monthlyRevenue.getOrderCount()).thenReturn(70L);
        when(paymentOrderRepository.summarizeMonthlyRevenue(PaymentOrderStatus.PAID))
                .thenReturn(List.of(monthlyRevenue));

        RevenueAnalyticsResponse response = paymentService.getRevenueAnalytics();

        assertEquals(100L, response.getTotalOrders());
        assertEquals(5_000_000L, response.getTotalRevenueVnd());
        assertEquals("2026-08", response.getMonthlyRevenue().getFirst().getMonth());
        verify(paymentOrderRepository, never()).findAll();
    }
}
