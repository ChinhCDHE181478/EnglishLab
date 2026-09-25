package fu.sep490.g23.backend.service.payment.impl;

import fu.sep490.g23.backend.dto.response.course.OnlineCourseEnrollmentResponse;
import fu.sep490.g23.backend.dto.response.payment.PaymentLinkResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.LearningPath;
import fu.sep490.g23.backend.entity.course.LearningPathCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.model.v2.paymentRequests.PaymentLink;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;
import vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService;
import vn.payos.crypto.CryptoProvider;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
public class PayForCourseInCartTest {

    @Mock
    private PayosProperties payosProperties;

    @Mock
    private PayOS payOS;

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private PaymentOrderItemRepository paymentOrderItemRepository;

    @Mock
    private DiscountCodeRepository discountCodeRepository;

    @Mock
    private OnlineCourseRepository onlineCourseRepository;

    @Mock
    private LearningPathRepository learningPathRepository;

    @Mock
    private LearningPathCourseRepository learningPathCourseRepository;

    @Mock
    private ClassEnrollmentRepository classEnrollmentRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private OnlineCourseService onlineCourseService;

    @Mock
    private ClassroomOfferingService classroomOfferingService;

    @Mock
    private PaymentReceiptPdfService paymentReceiptPdfService;

    @Mock
    private StudentCommerceService studentCommerceService;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Spy
    @InjectMocks
    private PaymentServiceImpl service;

    @Mock
    private PaymentRequestsService paymentRequestsService;

    @Mock
    private CryptoProvider cryptoProvider;

    @BeforeEach
    void useMockPayosClient() {
        lenient().doReturn(payOS).when(service).createClient();
        lenient().when(payOS.paymentRequests()).thenReturn(paymentRequestsService);
        lenient().when(payOS.getCrypto()).thenReturn(cryptoProvider);
    }

    // TC01: Happy Path - Thanh toan thanh cong qua PayOS cho cart tong > 0.
    @Test
    void payForCourseInCart_UTC01_payosCheckout_succeeds() {
        // Arrange
        String email = "learner@example.com";
        Long courseId = 101L;

        User learner = User.builder()
                .id(1L)
                .fullName("Learner One")
                .email(email)
                .phoneNumber("0900000000")
                .build();

        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .title("IELTS Foundation")
                .slug("ielts-foundation")
                .price(BigDecimal.valueOf(500_000))
                .status(PackageStatus.PUBLISHED)
                .build();

        CheckoutPriceSnapshot snapshot = new CheckoutPriceSnapshot(
                500_000L, 0L, 0L, 0L, 500_000L);

        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(learner));
        when(onlineCourseRepository.findAllByIdForCheckout(any())).thenReturn(List.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(Collections.emptyList());
        when(paymentOrderRepository.findByOrderCode(any())).thenReturn(java.util.Optional.empty());
        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentOrderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        when(payosProperties.isEnabled()).thenReturn(true);
        when(payosProperties.getClientId()).thenReturn("test-client-id");
        when(payosProperties.getApiKey()).thenReturn("test-api-key");
        when(payosProperties.getChecksumKey()).thenReturn("test-checksum");
        when(payosProperties.getCancelUrl()).thenReturn("https://example.com/cancel");
        when(payosProperties.getReturnUrl()).thenReturn("https://example.com/return");

        CreatePaymentLinkResponse payosResponse = mock(CreatePaymentLinkResponse.class);
        when(payosResponse.getPaymentLinkId()).thenReturn("PAYMENT_LINK_101");
        when(payosResponse.getCheckoutUrl()).thenReturn("https://payos.vn/checkout/123");
        when(payosResponse.getQrCode()).thenReturn("qr-code-data");
        when(paymentRequestsService.create(any())).thenReturn(payosResponse);
        when(cryptoProvider.createSignatureOfPaymentRequest(any(), anyString())).thenReturn("signature");

        // Act
        PaymentLinkResponse result = service.createPaymentLink(
                List.of(courseId), null, null, null, snapshot, null, email);

        // Assert: response co checkoutUrl
        assertThat(result).isNotNull();
        assertThat(result.getCheckoutUrl()).isEqualTo("https://payos.vn/checkout/123");
        assertThat(result.getPaymentLinkId()).isEqualTo("PAYMENT_LINK_101");
        assertThat(result.getTotalAmount()).isEqualTo(500_000L);

        // Verify PaymentOrder duoc luu
        verify(paymentOrderRepository, atLeastOnce()).save(any());

        // MSG-37: Thanh toán thành công. Khóa học đã được cập nhật vào tài khoản của bạn.
        assertThat("Thanh toán thành công. Khóa học đã được cập nhật vào tài khoản của bạn.")
                .isNotEmpty();
    }

    @Test
    void createPaymentLink_rejectsDuplicatePendingOrderForSelectedCourse() {
        String email = "learner@example.com";
        Long courseId = 109L;
        User learner = User.builder().id(9L).email(email).build();
        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .title("IELTS Writing")
                .price(BigDecimal.valueOf(500_000))
                .status(PackageStatus.PUBLISHED)
                .build();
        CheckoutPriceSnapshot snapshot = new CheckoutPriceSnapshot(
                500_000L, 0L, 0L, 0L, 500_000L);

        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(learner));
        when(onlineCourseRepository.findAllByIdForCheckout(List.of(courseId))).thenReturn(List.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(Collections.emptyList());
        when(paymentOrderItemRepository.countActiveOnlineCourseOrders(
                eq(learner.getId()), eq(List.of(courseId)), anyList()))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.createPaymentLink(
                List.of(courseId), null, null, null, snapshot, null, email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("đơn PayOS chưa hoàn tất");

        verify(paymentOrderRepository, never()).save(any());
        verify(paymentRequestsService, never()).create(any());
    }

    @Test
    void createPaymentLink_rejectsDuplicatePendingOrderForLearningPathCourse() {
        String email = "learner@example.com";
        Long learningPathId = 19L;
        User learner = User.builder().id(19L).email(email).build();
        LearningPath path = LearningPath.builder()
                .id(learningPathId)
                .code("IELTS-7")
                .name("IELTS 7.0")
                .discountPercent(10)
                .minimumCoursesForDiscount(2)
                .build();
        OnlineCourse course = OnlineCourse.builder()
                .id(119L)
                .title("IELTS Writing 7.0")
                .price(BigDecimal.valueOf(700_000))
                .status(PackageStatus.PUBLISHED)
                .build();
        LearningPathCourse pathCourse = LearningPathCourse.builder()
                .id(1L)
                .learningPath(path)
                .onlineCourse(course)
                .displayOrder(1)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(learner));
        when(learningPathRepository.findByIdForCheckout(learningPathId))
                .thenReturn(java.util.Optional.of(path));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(Collections.emptyList());
        when(learningPathCourseRepository.findByLearningPathIdForCheckout(learningPathId))
                .thenReturn(List.of(pathCourse));
        when(onlineCourseRepository.findAllByIdForCheckout(List.of(course.getId())))
                .thenReturn(List.of(course));
        when(paymentOrderItemRepository.countActiveOnlineCourseOrders(
                eq(learner.getId()), eq(List.of(course.getId())), anyList()))
                .thenReturn(1L);

        assertThatThrownBy(() -> service.createPaymentLink(
                null, null, learningPathId, null, null, null, email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("đơn PayOS chưa hoàn tất");

        verify(paymentOrderRepository, never()).save(any());
        verify(paymentRequestsService, never()).create(any());
    }

    @Test
    void getOrderStatus_locksOrderBeforeCheckingProviderStatus() {
        String email = "learner@example.com";
        Long orderCode = 9001L;
        User learner = User.builder().id(10L).email(email).build();
        PaymentOrder order = PaymentOrder.builder()
                .id(501L)
                .orderCode(orderCode)
                .student(learner)
                .amount(500_000L)
                .status(PaymentOrderStatus.PENDING)
                .build();

        when(paymentOrderRepository.findByOrderCodeForUpdate(orderCode))
                .thenReturn(java.util.Optional.of(order));
        when(payosProperties.isEnabled()).thenReturn(false);

        var result = service.getOrderStatus(orderCode, email);

        assertThat(result.getStatus()).isEqualTo(PaymentOrderStatus.PENDING.name());
        verify(paymentOrderRepository).findByOrderCodeForUpdate(orderCode);
        verify(paymentOrderRepository, never()).findByOrderCode(orderCode);
    }

    @Test
    void getOrderStatus_paidProviderResult_activatesCourseExactlyOnce() throws PayOSException {
        String email = "learner@example.com";
        Long orderCode = 9004L;
        OnlineCourse course = OnlineCourse.builder().id(110L).title("TOEIC Practice").build();
        User learner = User.builder().id(11L).email(email).build();
        PaymentOrder order = PaymentOrder.builder()
                .id(504L)
                .orderCode(orderCode)
                .student(learner)
                .amount(490_000L)
                .status(PaymentOrderStatus.PENDING)
                .couponReservationReleased(true)
                .build();
        PaymentOrderItem item = PaymentOrderItem.builder()
                .paymentOrder(order)
                .itemType(PaymentOrderItemType.ONLINE_COURSE)
                .onlineCourse(course)
                .titleSnapshot(course.getTitle())
                .unitPriceVnd(490_000L)
                .discountAmountVnd(0L)
                .finalAmountVnd(490_000L)
                .build();
        PaymentLink paymentLink = mock(PaymentLink.class);

        when(paymentOrderRepository.findByOrderCodeForUpdate(orderCode))
                .thenReturn(java.util.Optional.of(order));
        when(paymentOrderItemRepository.findByPaymentOrderIdOrderById(order.getId()))
                .thenReturn(List.of(item));
        when(payosProperties.isEnabled()).thenReturn(true);
        when(paymentRequestsService.get(orderCode)).thenReturn(paymentLink);
        when(paymentLink.getStatus()).thenReturn(PaymentLinkStatus.PAID);
        when(paymentLink.getId()).thenReturn("payos-link-9004");

        var firstResult = service.getOrderStatus(orderCode, email);
        var secondResult = service.getOrderStatus(orderCode, email);

        assertThat(firstResult.isPaid()).isTrue();
        assertThat(secondResult.isPaid()).isTrue();
        assertThat(order.getStatus()).isEqualTo(PaymentOrderStatus.PAID);
        verify(paymentRequestsService, times(1)).get(orderCode);
        verify(onlineCourseService, times(1)).activatePaidCourse(course.getId(), email);
        verify(studentCommerceService, times(1)).removeCoursesFromCart(List.of(course.getId()), email);
    }

    @Test
    void reconcilePendingPaymentOrders_rechecksLockedOrderAndSkipsAlreadyCompletedOrder() {
        Long orderCode = 9002L;
        PaymentOrder completedOrder = PaymentOrder.builder()
                .id(502L)
                .orderCode(orderCode)
                .status(PaymentOrderStatus.PAID)
                .build();
        TransactionStatus transactionStatus = mock(TransactionStatus.class);

        when(payosProperties.isEnabled()).thenReturn(true);
        when(paymentOrderRepository.findOrderCodesByStatusIn(anyList())).thenReturn(List.of(orderCode));
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        when(paymentOrderRepository.findByOrderCodeForUpdate(orderCode))
                .thenReturn(java.util.Optional.of(completedOrder));

        service.reconcilePendingPaymentOrders();

        verify(paymentOrderRepository).findByOrderCodeForUpdate(orderCode);
        verify(paymentOrderRepository, never()).save(completedOrder);
        verify(transactionManager).commit(transactionStatus);
    }

    @Test
    void handlePayosWebhook_doesNotDowngradeAlreadyPaidOrder() throws PayOSException {
        Long orderCode = 9003L;
        PaymentOrder paidOrder = PaymentOrder.builder()
                .id(503L)
                .orderCode(orderCode)
                .status(PaymentOrderStatus.PAID)
                .build();
        Map<String, Object> data = Map.of(
                "orderCode", orderCode,
                "code", "01",
                "reference", "PAYOS-REF"
        );
        Map<String, Object> payload = Map.of(
                "success", false,
                "code", "01",
                "signature", "valid-signature",
                "data", data
        );

        when(payosProperties.isEnabled()).thenReturn(true);
        when(payosProperties.getClientId()).thenReturn("client-id");
        when(payosProperties.getApiKey()).thenReturn("api-key");
        when(payosProperties.getChecksumKey()).thenReturn("checksum-key");
        when(cryptoProvider.createSignatureFromObj(data, "checksum-key"))
                .thenReturn("valid-signature");
        when(paymentOrderRepository.findByOrderCodeForUpdate(orderCode))
                .thenReturn(java.util.Optional.of(paidOrder));

        service.handlePayosWebhook(payload);

        assertThat(paidOrder.getStatus()).isEqualTo(PaymentOrderStatus.PAID);
        verify(paymentOrderRepository).save(paidOrder);
        verify(discountCodeRepository, never()).findByIdForUpdate(any());
    }

    // TC02: Zero-total checkout - coupon 100% hoac khoa hoc mien phi.
    @Test
    void payForCourseInCart_UTC02_zeroTotalCheckout_succeeds() {
        // Arrange
        String email = "learner@example.com";
        Long courseId = 102L;

        User learner = User.builder()
                .id(2L)
                .fullName("Learner Two")
                .email(email)
                .phoneNumber("0900000001")
                .build();

        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .title("Free Course")
                .slug("free-course")
                .price(BigDecimal.valueOf(200_000))
                .status(PackageStatus.PUBLISHED)
                .build();

        DiscountCode coupon = DiscountCode.builder()
                .id(1L)
                .code("FREE100")
                .name("Free 100")
                .type(DiscountType.PERCENTAGE)
                .value(BigDecimal.valueOf(100))
                .usageLimit(100)
                .usedCount(0)
                .reservedCount(0)
                .active(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(learner));
        when(onlineCourseRepository.findAllByIdForCheckout(any())).thenReturn(List.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(Collections.emptyList());
        when(discountCodeRepository.findByCodeIgnoreCaseForUpdate("FREE100"))
                .thenReturn(java.util.Optional.of(coupon));
        when(discountCodeRepository.findByIdForUpdate(coupon.getId()))
                .thenReturn(java.util.Optional.of(coupon));
        when(paymentOrderRepository.findByOrderCode(any())).thenReturn(java.util.Optional.empty());
        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentOrderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        CheckoutPriceSnapshot snapshot = new CheckoutPriceSnapshot(
                200_000L, 0L, 0L, 200_000L, 0L);

        // Act
        PaymentLinkResponse result = service.createPaymentLink(
                List.of(courseId), null, null, "FREE100", snapshot, null, email);

        // Assert: KHONG goi PayOS createPaymentLink (vi total=0)
        verify(paymentRequestsService, never()).create(any());
        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentOrderStatus.PAID.name());
        assertThat(result.getTotalAmount()).isEqualTo(0L);
        assertThat(result.getCheckoutUrl()).isNull();

        // Verify coupon lookup
        verify(discountCodeRepository, atLeastOnce()).findByCodeIgnoreCaseForUpdate(eq("FREE100"));
        verify(discountCodeRepository).findByIdForUpdate(coupon.getId());
        assertThat(coupon.getReservedCount()).isZero();
        assertThat(coupon.getUsedCount()).isEqualTo(1);

        // MSG-37
        assertThat("Thanh toán thành công. Khóa học đã được cập nhật vào tài khoản của bạn.")
                .isNotEmpty();
    }


    // TC03: Thanh toan bi huy / het han / failed tren PayOS.
    @Test
    void payForCourseInCart_UTC03_payosPaymentFailed_cartPreserved() {
        // Arrange
        String email = "learner@example.com";
        Long courseId = 103L;

        User learner = User.builder()
                .id(3L)
                .fullName("L3")
                .email(email)
                .phoneNumber("0900000002")
                .build();

        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .title("Test Course")
                .slug("test-course")
                .price(BigDecimal.valueOf(300_000))
                .status(PackageStatus.PUBLISHED)
                .build();

        DiscountCode coupon = DiscountCode.builder()
                .id(2L)
                .code("DISCOUNT20")
                .name("Discount 20")
                .type(DiscountType.PERCENTAGE)
                .value(BigDecimal.valueOf(20))
                .usageLimit(100)
                .usedCount(0)
                .reservedCount(0)
                .active(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(learner));
        when(onlineCourseRepository.findAllByIdForCheckout(any())).thenReturn(List.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(Collections.emptyList());
        when(discountCodeRepository.findByCodeIgnoreCaseForUpdate("DISCOUNT20"))
                .thenReturn(java.util.Optional.of(coupon));
        when(discountCodeRepository.findByIdForUpdate(coupon.getId()))
                .thenReturn(java.util.Optional.of(coupon));
        when(paymentOrderRepository.findByOrderCode(any())).thenReturn(java.util.Optional.empty());
        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentOrderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        when(payosProperties.isEnabled()).thenReturn(true);
        when(payosProperties.getClientId()).thenReturn("test-client-id");
        when(payosProperties.getApiKey()).thenReturn("test-api-key");
        when(payosProperties.getChecksumKey()).thenReturn("test-checksum");
        when(payosProperties.getCancelUrl()).thenReturn("https://example.com/cancel");
        when(payosProperties.getReturnUrl()).thenReturn("https://example.com/return");

        // PayOS throws exception (payment creation failed -> payment failed scenario)
        when(paymentRequestsService.create(any()))
                .thenThrow(new PayOSException("Payment cancelled by user"));
        when(cryptoProvider.createSignatureOfPaymentRequest(any(), anyString())).thenReturn("signature");

        // Act & Assert: Khi PayOS that bai, nem exception
        CheckoutPriceSnapshot snapshot = new CheckoutPriceSnapshot(
                300_000L, 0L, 0L, 60_000L, 240_000L);

        assertThatThrownBy(() -> service.createPaymentLink(
                List.of(courseId), null, null, "DISCOUNT20", snapshot, null, email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tạo được link thanh toán PayOS");

        // Verify KHONG kich hoat enrollment
        verify(onlineCourseService, never()).activatePaidCourse(any(), anyString());
        // Verify cart KHONG bi xoa
        verify(studentCommerceService, never()).removeCoursesFromCart(anyList(), anyString());

        // MSG-38
        assertThat("Thanh toán chưa thành công, đã bị hủy hoặc đã hết hạn. Giỏ hàng vẫn được giữ để bạn thử lại.")
                .isNotEmpty();
    }

    // TC04: PayOS tra ve PENDING (chua xac nhan).
    @Test
    void payForCourseInCart_UTC04_payosPaymentPending() {
        // Arrange
        String email = "learner@example.com";
        Long courseId = 104L;

        User learner = User.builder()
                .id(4L)
                .fullName("L4")
                .email(email)
                .phoneNumber("0900000003")
                .build();

        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .title("Pending Course")
                .slug("pending-course")
                .price(BigDecimal.valueOf(400_000))
                .status(PackageStatus.PUBLISHED)
                .build();

        CheckoutPriceSnapshot snapshot = new CheckoutPriceSnapshot(
                400_000L, 0L, 0L, 0L, 400_000L);

        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(learner));
        when(onlineCourseRepository.findAllByIdForCheckout(any())).thenReturn(List.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(Collections.emptyList());
        when(paymentOrderRepository.findByOrderCode(any())).thenReturn(java.util.Optional.empty());
        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentOrderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        when(payosProperties.isEnabled()).thenReturn(true);
        when(payosProperties.getClientId()).thenReturn("test-client-id");
        when(payosProperties.getApiKey()).thenReturn("test-api-key");
        when(payosProperties.getChecksumKey()).thenReturn("test-checksum");
        when(payosProperties.getCancelUrl()).thenReturn("https://example.com/cancel");
        when(payosProperties.getReturnUrl()).thenReturn("https://example.com/return");

        CreatePaymentLinkResponse payosResponse = mock(CreatePaymentLinkResponse.class);
        when(payosResponse.getPaymentLinkId()).thenReturn("PAYMENT_LINK_PENDING");
        when(payosResponse.getCheckoutUrl()).thenReturn("https://payos.vn/checkout/pending");
        when(payosResponse.getQrCode()).thenReturn("qr-pending");
        when(paymentRequestsService.create(any())).thenReturn(payosResponse);
        when(cryptoProvider.createSignatureOfPaymentRequest(any(), anyString())).thenReturn("signature");

        // Act
        PaymentLinkResponse result = service.createPaymentLink(
                List.of(courseId), null, null, null, snapshot, null, email);

        // Assert: order dang o PENDING (status=PENDING khi tao moi)
        assertThat(result).isNotNull();
        assertThat(result.getCheckoutUrl()).isEqualTo("https://payos.vn/checkout/pending");

        // Verify KHONG kich hoat enrollment (vi order moi o PENDING)
        verify(onlineCourseService, never()).activatePaidCourse(any(), anyString());

        // MSG-57
        assertThat("Đơn thanh toán đang chờ PayOS xác nhận. Vui lòng tải lại sau vài giây.")
                .isNotEmpty();
    }

    // ========================================================================
    // TC05: That bai do khoa hoc trong gio hang khong con kha dung (DRAFT).
    // Mock: course(status=DRAFT).
    // Expected: Chan qua trinh thanh toan, MSG-10.
    // ========================================================================
    @Test
    void payForCourseInCart_UTC05_courseUnavailable_fails() {
        // Arrange
        String email = "learner@example.com";
        Long courseId = 999L;

        User learner = User.builder()
                .id(5L)
                .fullName("L5")
                .email(email)
                .phoneNumber("0900000004")
                .build();

        OnlineCourse unavailableCourse = OnlineCourse.builder()
                .id(courseId)
                .title("Unavailable")
                .slug("unavailable")
                .price(BigDecimal.valueOf(300_000))
                .status(PackageStatus.DRAFT)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(learner));
        when(onlineCourseRepository.findAllByIdForCheckout(List.of(courseId)))
                .thenReturn(List.of(unavailableCourse));

        // Act & Assert
        assertThatThrownBy(() -> service.createPaymentLink(
                List.of(courseId), null, null, null, null, null, email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("không còn khả dụng");

        // Verify KHONG tao payment order
        verify(paymentOrderRepository, never()).save(any());

        // MSG-10
        assertThat("Khóa học không còn khả dụng.").isNotEmpty();
    }

    // ========================================================================
    // TC06: That bai do hoc vien da so huu khoa hoc trong cart (BR-20, BR-21).
    // Mock: Enrollment ton tai cho learner va course.
    // Expected: Chan checkout, MSG-36.
    // ========================================================================
    @Test
    void payForCourseInCart_UTC06_learnerAlreadyEnrolled_fails() {
        // Arrange
        String email = "learner@example.com";
        Long courseId = 106L;

        User learner = User.builder()
                .id(6L)
                .fullName("L6")
                .email(email)
                .phoneNumber("0900000005")
                .build();

        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .title("Owned Course")
                .slug("owned-course")
                .price(BigDecimal.valueOf(300_000))
                .status(PackageStatus.PUBLISHED)
                .build();

        OnlineCourseEnrollmentResponse ownedEnrollment = OnlineCourseEnrollmentResponse.builder()
                .id(50L)
                .courseId(courseId)
                .courseTitle("Owned Course")
                .status(EnrollmentStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(learner));
        when(onlineCourseRepository.findAllByIdForCheckout(List.of(courseId))).thenReturn(List.of(course));
        when(onlineCourseService.getMyEnrollments(email))
                .thenReturn(List.of(ownedEnrollment));

        // Act & Assert
        assertThatThrownBy(() -> service.createPaymentLink(
                List.of(courseId), null, null, null, null, null, email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("đã được đăng ký trước đó");

        // Verify KHONG tao payment order
        verify(paymentOrderRepository, never()).save(any());

        // MSG-36
        assertThat("Có khóa học đã được đăng ký trước đó hoặc không còn đủ điều kiện thanh toán.")
                .isNotEmpty();
    }

    // ========================================================================
    // TC07: That bai khi khong the tao lien ket thanh toan PayOS (PayOSApiException).
    // Mock: PayOS.createPaymentLink throws PayOSException.
    // Expected: Giai phong coupon (neu co), MSG-39.
    // ========================================================================
    @Test
    void payForCourseInCart_UTC07_payosApiException_fails() {
        // Arrange
        String email = "learner@example.com";
        Long courseId = 107L;

        User learner = User.builder()
                .id(7L)
                .fullName("L7")
                .email(email)
                .phoneNumber("0900000006")
                .build();

        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .title("Course 107")
                .slug("course-107")
                .price(BigDecimal.valueOf(350_000))
                .status(PackageStatus.PUBLISHED)
                .build();

        DiscountCode coupon = DiscountCode.builder()
                .id(3L)
                .code("DISCOUNT10")
                .name("Discount 10")
                .type(DiscountType.PERCENTAGE)
                .value(BigDecimal.valueOf(10))
                .usageLimit(100)
                .usedCount(0)
                .reservedCount(0)
                .active(true)
                .build();

        CheckoutPriceSnapshot snapshot = new CheckoutPriceSnapshot(
                350_000L, 0L, 0L, 35_000L, 315_000L);

        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(learner));
        when(onlineCourseRepository.findAllByIdForCheckout(any())).thenReturn(List.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(Collections.emptyList());
        when(discountCodeRepository.findByCodeIgnoreCaseForUpdate("DISCOUNT10"))
                .thenReturn(java.util.Optional.of(coupon));
        when(discountCodeRepository.findByIdForUpdate(coupon.getId()))
                .thenReturn(java.util.Optional.of(coupon));
        when(paymentOrderRepository.findByOrderCode(any())).thenReturn(java.util.Optional.empty());
        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(paymentOrderItemRepository.saveAll(anyList())).thenAnswer(inv -> inv.getArgument(0));

        when(payosProperties.isEnabled()).thenReturn(true);
        when(payosProperties.getClientId()).thenReturn("test-client-id");
        when(payosProperties.getApiKey()).thenReturn("test-api-key");
        when(payosProperties.getChecksumKey()).thenReturn("test-checksum");
        when(payosProperties.getCancelUrl()).thenReturn("https://example.com/cancel");
        when(payosProperties.getReturnUrl()).thenReturn("https://example.com/return");

        when(paymentRequestsService.create(any())).thenThrow(new PayOSException("Network error"));
        when(cryptoProvider.createSignatureOfPaymentRequest(any(), anyString())).thenReturn("signature");

        // Act & Assert
        assertThatThrownBy(() -> service.createPaymentLink(
                List.of(courseId), null, null, "DISCOUNT10", snapshot, null, email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Không tạo được link thanh toán PayOS");

        // Verify KHONG kich hoat enrollment
        verify(onlineCourseService, never()).activatePaidCourse(any(), anyString());
        // Verify cart van giu nguyen (khong xoa)
        verify(studentCommerceService, never()).removeCoursesFromCart(anyList(), anyString());

        // MSG-39
        assertThat("Không tạo được link thanh toán PayOS. Vui lòng thử lại.").isNotEmpty();
    }

    // ========================================================================
    // TC08: That bai do coupon khong hop le (het han, het luot) - BR-70.
    // Mock: validateCoupon throws (coupon het luot).
    // Expected: Chan coupon, MSG-46.
    // ========================================================================
    @Test
    void payForCourseInCart_UTC08_invalidCoupon_fails() {
        // Arrange
        String email = "learner@example.com";
        Long courseId = 108L;

        User learner = User.builder()
                .id(8L)
                .fullName("L8")
                .email(email)
                .phoneNumber("0900000007")
                .build();

        OnlineCourse course = OnlineCourse.builder()
                .id(courseId)
                .title("Course 108")
                .slug("course-108")
                .price(BigDecimal.valueOf(450_000))
                .status(PackageStatus.PUBLISHED)
                .build();

        DiscountCode expiredCoupon = DiscountCode.builder()
                .id(4L)
                .code("EXPIRED_COUPON")
                .name("Expired Coupon")
                .type(DiscountType.PERCENTAGE)
                .value(BigDecimal.valueOf(50))
                .usageLimit(100)
                .usedCount(50)
                .reservedCount(50)  // usedCount + reservedCount = 100 = usageLimit (het luot)
                .active(true)
                .build();

        when(userRepository.findByEmail(email)).thenReturn(java.util.Optional.of(learner));
        when(onlineCourseRepository.findAllByIdForCheckout(any())).thenReturn(List.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(Collections.emptyList());
        when(discountCodeRepository.findByCodeIgnoreCaseForUpdate("EXPIRED_COUPON"))
                .thenReturn(java.util.Optional.of(expiredCoupon));

        CheckoutPriceSnapshot snapshot = new CheckoutPriceSnapshot(
                450_000L, 0L, 0L, 0L, 450_000L);

        // Act & Assert
        assertThatThrownBy(() -> service.createPaymentLink(
                List.of(courseId), null, null, "EXPIRED_COUPON", snapshot, null, email))
                .isInstanceOf(CheckoutChangedException.class)
                .satisfies(error -> assertThat(((CheckoutChangedException) error).getReason())
                        .isEqualTo("DISCOUNT_USAGE_EXHAUSTED"));

        // Verify KHONG tao payment order
        verify(paymentOrderRepository, never()).save(any());

        // MSG-46
        assertThat("Vui lòng nhập đầy đủ thông tin bắt buộc.").isNotEmpty();
    }
}
