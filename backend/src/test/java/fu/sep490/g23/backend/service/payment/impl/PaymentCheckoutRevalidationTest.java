package fu.sep490.g23.backend.service.payment.impl;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.payment.DiscountCode;
import fu.sep490.g23.backend.entity.payment.PaymentOrder;
import fu.sep490.g23.backend.entity.payment.PaymentOrderItem;
import fu.sep490.g23.backend.entity.payment.enums.DiscountType;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderItemType;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import fu.sep490.g23.backend.exception.CheckoutChangedException;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.LearningPathCourseRepository;
import fu.sep490.g23.backend.repository.course.LearningPathRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.payment.DiscountCodeRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderItemRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sep490.g23.backend.service.commerce.StudentCommerceService;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import fu.sep490.g23.backend.service.payment.CheckoutPriceSnapshot;
import fu.sep490.g23.backend.service.payment.PaymentReceiptPdfService;
import fu.sep490.g23.backend.service.payment.PayosProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import vn.payos.PayOS;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentCheckoutRevalidationTest {

    private static final String EMAIL = "learner@example.com";
    private static final long COURSE_ID = 100L;

    @Mock private PayosProperties payosProperties;
    @Mock private PaymentOrderRepository paymentOrderRepository;
    @Mock private PaymentOrderItemRepository paymentOrderItemRepository;
    @Mock private DiscountCodeRepository discountCodeRepository;
    @Mock private OnlineCourseRepository onlineCourseRepository;
    @Mock private LearningPathRepository learningPathRepository;
    @Mock private LearningPathCourseRepository learningPathCourseRepository;
    @Mock private ClassEnrollmentRepository classEnrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private OnlineCourseService onlineCourseService;
    @Mock private ClassroomOfferingService classroomOfferingService;
    @Mock private PaymentReceiptPdfService paymentReceiptPdfService;
    @Mock private StudentCommerceService studentCommerceService;
    @Mock private PayOS payOS;

    @Spy
    @InjectMocks
    private PaymentServiceImpl service;

    @Test
    void matchingCheckoutCreatesOrderAndPayosLink() throws Exception {
        OnlineCourse course = course(500_000L);
        arrangeCourse(course);
        arrangePayos();

        var response = service.createPaymentLink(
                List.of(COURSE_ID), List.of(), null, null, snapshot(500_000L), EMAIL);

        assertThat(response.getCheckoutUrl()).isEqualTo("https://pay.payos.vn/web/123");
        assertThat(response.getTotalAmount()).isEqualTo(500_000L);
        verify(paymentOrderRepository, times(2)).save(any(PaymentOrder.class));
        verify(payOS).paymentRequests();
    }

    @Test
    void increasedPriceRejectsBeforeOrderAndPayos() {
        arrangeCourse(course(600_000L));

        assertCheckoutChanged(snapshot(500_000L), null, "PRICE_CHANGED");
    }

    @Test
    void decreasedPriceRejectsBeforeOrderAndPayos() {
        arrangeCourse(course(400_000L));

        assertCheckoutChanged(snapshot(500_000L), null, "PRICE_CHANGED");
    }

    @Test
    void expiredDiscountReturnsCheckoutChanged() {
        arrangeCourse(course(500_000L));
        DiscountCode coupon = percentageCoupon("SAVE20", 20);
        coupon.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(discountCodeRepository.findByCodeIgnoreCaseForUpdate("SAVE20")).thenReturn(Optional.of(coupon));

        assertCheckoutChanged(new CheckoutPriceSnapshot(500_000L, 0L, 0L, 100_000L, 400_000L), "SAVE20", "DISCOUNT_EXPIRED");
    }

    @Test
    void exhaustedDiscountReturnsCheckoutChanged() {
        arrangeCourse(course(500_000L));
        DiscountCode coupon = percentageCoupon("LIMITED", 20);
        coupon.setUsageLimit(1);
        coupon.setUsedCount(1);
        when(discountCodeRepository.findByCodeIgnoreCaseForUpdate("LIMITED")).thenReturn(Optional.of(coupon));

        assertCheckoutChanged(new CheckoutPriceSnapshot(500_000L, 0L, 0L, 100_000L, 400_000L), "LIMITED", "DISCOUNT_USAGE_EXHAUSTED");
    }

    @Test
    void changedDiscountAmountRejectsEvenWhenCoursePriceIsStable() {
        arrangeCourse(course(500_000L));
        DiscountCode coupon = percentageCoupon("SAVE20", 20);
        when(discountCodeRepository.findByCodeIgnoreCaseForUpdate("SAVE20")).thenReturn(Optional.of(coupon));

        assertCheckoutChanged(new CheckoutPriceSnapshot(500_000L, 0L, 0L, 50_000L, 450_000L), "SAVE20", "DISCOUNT_CHANGED");
    }

    @Test
    void retryWithRefreshedCheckoutCreatesPayosLink() throws Exception {
        arrangeCourse(course(600_000L));
        assertCheckoutChanged(snapshot(500_000L), null, "PRICE_CHANGED");
        arrangePayos();

        var response = service.createPaymentLink(
                List.of(COURSE_ID), List.of(), null, null, snapshot(600_000L), EMAIL);

        assertThat(response.getTotalAmount()).isEqualTo(600_000L);
        verify(payOS).paymentRequests();
    }

    @Test
    void createdOrderKeepsSnapshotWhenCoursePriceChangesLater() throws Exception {
        OnlineCourse course = course(500_000L);
        arrangeCourse(course);
        arrangePayos();

        service.createPaymentLink(List.of(COURSE_ID), List.of(), null, null, snapshot(500_000L), EMAIL);
        ArgumentCaptor<PaymentOrder> captor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(paymentOrderRepository, times(2)).save(captor.capture());
        PaymentOrder createdOrder = captor.getAllValues().getFirst();

        course.setPrice(BigDecimal.valueOf(900_000L));

        assertThat(createdOrder.getAmount()).isEqualTo(500_000L);
        assertThat(createdOrder.getOriginalAmount()).isEqualTo(500_000L);
    }

    @Test
    void duplicateSuccessfulWebhookDoesNotDuplicateEnrollment() throws Exception {
        OnlineCourse course = course(500_000L);
        User learner = learner();
        PaymentOrder order = PaymentOrder.builder()
                .id(8L)
                .orderCode(1700000000001L)
                .student(learner)
                .amount(500_000L)
                .status(PaymentOrderStatus.PENDING)
                .couponReservationReleased(true)
                .build();
        PaymentOrderItem item = PaymentOrderItem.builder()
                .itemType(PaymentOrderItemType.ONLINE_COURSE)
                .onlineCourse(course)
                .titleSnapshot(course.getTitle())
                .build();
        when(paymentOrderRepository.findByOrderCodeForUpdate(order.getOrderCode())).thenReturn(Optional.of(order));
        when(paymentOrderItemRepository.findByPaymentOrderIdOrderById(order.getId())).thenReturn(List.of(item));
        when(payosProperties.isEnabled()).thenReturn(true);
        when(payosProperties.getClientId()).thenReturn("client");
        when(payosProperties.getApiKey()).thenReturn("api-key");
        when(payosProperties.getChecksumKey()).thenReturn("checksum");
        var crypto = org.mockito.Mockito.mock(vn.payos.crypto.CryptoProvider.class);
        when(payOS.getCrypto()).thenReturn(crypto);
        when(crypto.createSignatureFromObj(any(), anyString())).thenReturn("sig");
        doReturn(payOS).when(service).createClient();
        Map<String, Object> payload = Map.of(
                "success", true,
                "code", "00",
                "signature", "sig",
                "data", Map.of("orderCode", order.getOrderCode(), "code", "00")
        );

        service.handlePayosWebhook(payload);
        service.handlePayosWebhook(payload);

        verify(onlineCourseService, times(1)).activatePaidCourse(COURSE_ID, EMAIL);
        verify(studentCommerceService, times(1)).removeCoursesFromCart(List.of(COURSE_ID), EMAIL);
        assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.PAID);
    }

    @Test
    void incompleteCheckoutSnapshotIsRejectedBeforeOrder() {
        arrangeCourse(course(500_000L));
        CheckoutPriceSnapshot incomplete = new CheckoutPriceSnapshot(null, 0L, 0L, 0L, 500_000L);

        assertThatThrownBy(() -> service.createPaymentLink(
                List.of(COURSE_ID), List.of(), null, null, incomplete, EMAIL))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Thiếu thông tin giá checkout");
        verify(paymentOrderRepository, never()).save(any());
        verify(payOS, never()).paymentRequests();
    }

    private void assertCheckoutChanged(CheckoutPriceSnapshot snapshot, String couponCode, String reason) {
        assertThatThrownBy(() -> service.createPaymentLink(
                List.of(COURSE_ID), List.of(), null, couponCode, snapshot, EMAIL))
                .isInstanceOfSatisfying(CheckoutChangedException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(reason));
        verify(paymentOrderRepository, never()).save(any());
        verify(payOS, never()).paymentRequests();
    }

    private void arrangeCourse(OnlineCourse course) {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(learner()));
        when(onlineCourseRepository.findAllByIdForCheckout(List.of(COURSE_ID))).thenReturn(List.of(course));
        when(onlineCourseService.getMyEnrollments(EMAIL)).thenReturn(List.of());
    }

    private void arrangePayos() throws Exception {
        when(payosProperties.isEnabled()).thenReturn(true);
        when(payosProperties.getClientId()).thenReturn("client");
        when(payosProperties.getApiKey()).thenReturn("api-key");
        when(payosProperties.getChecksumKey()).thenReturn("checksum");
        when(payosProperties.getCancelUrl()).thenReturn("https://englishlab.io.vn/checkout");
        when(payosProperties.getReturnUrl()).thenReturn("https://englishlab.io.vn/checkout");
        when(paymentOrderRepository.findByOrderCode(any(Long.class))).thenReturn(Optional.empty());
        when(paymentOrderRepository.save(any(PaymentOrder.class))).thenAnswer(invocation -> {
            PaymentOrder order = invocation.getArgument(0);
            order.setId(9L);
            return order;
        });
        var paymentRequests = org.mockito.Mockito.mock(vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService.class);
        var response = org.mockito.Mockito.mock(vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse.class);
        var crypto = org.mockito.Mockito.mock(vn.payos.crypto.CryptoProvider.class);
        when(payOS.paymentRequests()).thenReturn(paymentRequests);
        when(payOS.getCrypto()).thenReturn(crypto);
        when(crypto.createSignatureOfPaymentRequest(any(), anyString())).thenReturn("signature");
        when(paymentRequests.create(any())).thenReturn(response);
        when(response.getPaymentLinkId()).thenReturn("link-123");
        when(response.getCheckoutUrl()).thenReturn("https://pay.payos.vn/web/123");
        when(response.getQrCode()).thenReturn("qr-123");
        doReturn(payOS).when(service).createClient();
    }

    private User learner() {
        return User.builder().id(1L).email(EMAIL).fullName("Learner").phoneNumber("0900000000").build();
    }

    private OnlineCourse course(long price) {
        return OnlineCourse.builder()
                .id(COURSE_ID)
                .title("IELTS Course")
                .slug("ielts-course")
                .price(BigDecimal.valueOf(price))
                .status(PackageStatus.PUBLISHED)
                .build();
    }

    private CheckoutPriceSnapshot snapshot(long amount) {
        return new CheckoutPriceSnapshot(amount, 0L, 0L, 0L, amount);
    }

    private DiscountCode percentageCoupon(String code, int percentage) {
        return DiscountCode.builder()
                .id(2L)
                .code(code)
                .name(code)
                .type(DiscountType.PERCENTAGE)
                .value(BigDecimal.valueOf(percentage))
                .usageLimit(100)
                .usedCount(0)
                .reservedCount(0)
                .active(true)
                .build();
    }
}
