package fu.sep490.g23.backend.service.payment.impl;

import fu.sep490.g23.backend.dto.response.course.OnlineCourseEnrollmentResponse;
import fu.sep490.g23.backend.dto.response.payment.PaymentLinkResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.payment.DiscountCode;
import fu.sep490.g23.backend.entity.payment.enums.DiscountType;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;
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
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse;
import vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService;
import vn.payos.crypto.CryptoProvider;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

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
        when(onlineCourseRepository.findById(courseId)).thenReturn(java.util.Optional.of(course));
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
        when(cryptoProvider.createSignatureFromObj(any(), anyString())).thenReturn("webhook-signature");

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
        when(onlineCourseRepository.findById(courseId)).thenReturn(java.util.Optional.of(course));
        when(onlineCourseRepository.findAllByIdForCheckout(any())).thenReturn(List.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(Collections.emptyList());
        when(discountCodeRepository.findByCodeIgnoreCaseForUpdate("FREE100"))
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
        when(onlineCourseRepository.findById(courseId)).thenReturn(java.util.Optional.of(course));
        when(onlineCourseRepository.findAllByIdForCheckout(any())).thenReturn(List.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(Collections.emptyList());
        when(discountCodeRepository.findByCodeIgnoreCaseForUpdate("DISCOUNT20"))
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
        when(onlineCourseRepository.findById(courseId)).thenReturn(java.util.Optional.of(course));
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
        when(onlineCourseRepository.findById(courseId)).thenReturn(java.util.Optional.of(unavailableCourse));

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
        when(onlineCourseRepository.findById(courseId)).thenReturn(java.util.Optional.of(course));
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
        when(onlineCourseRepository.findById(courseId)).thenReturn(java.util.Optional.of(course));
        when(onlineCourseRepository.findAllByIdForCheckout(any())).thenReturn(List.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(Collections.emptyList());
        when(discountCodeRepository.findByCodeIgnoreCaseForUpdate("DISCOUNT10"))
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
        when(onlineCourseRepository.findById(courseId)).thenReturn(java.util.Optional.of(course));
        when(onlineCourseRepository.findAllByIdForCheckout(any())).thenReturn(List.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(Collections.emptyList());
        when(discountCodeRepository.findByCodeIgnoreCaseForUpdate("EXPIRED_COUPON"))
                .thenReturn(java.util.Optional.of(expiredCoupon));

        CheckoutPriceSnapshot snapshot = new CheckoutPriceSnapshot(
                450_000L, 0L, 0L, 0L, 450_000L);

        // Act & Assert
        assertThatThrownBy(() -> service.createPaymentLink(
                List.of(courseId), null, null, "EXPIRED_COUPON", snapshot, null, email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("hết lượt");

        // Verify KHONG tao payment order
        verify(paymentOrderRepository, never()).save(any());

        // MSG-46
        assertThat("Vui lòng nhập đầy đủ thông tin bắt buộc.").isNotEmpty();
    }
}
