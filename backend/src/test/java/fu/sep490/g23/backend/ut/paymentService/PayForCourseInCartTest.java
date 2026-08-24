package fu.sep490.g23.backend.ut.paymentService;

import fu.sep490.g23.backend.dto.response.payment.PaymentLinkResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.payment.DiscountCode;
import fu.sep490.g23.backend.entity.payment.PaymentOrder;
import fu.sep490.g23.backend.entity.payment.enums.DiscountType;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.payment.DiscountCodeRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import fu.sep490.g23.backend.service.payment.PaymentReceiptPdfService;
import fu.sep490.g23.backend.service.payment.PayosProperties;
import fu.sep490.g23.backend.service.payment.impl.PaymentServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.payos.PayOS;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PayForCourseInCartTest {

    @Mock private PayosProperties payosProperties;
    @Mock private PayOS payOS;
    @Mock private PaymentOrderRepository paymentOrderRepository;
    @Mock private DiscountCodeRepository discountCodeRepository;
    @Mock private OnlineCourseRepository onlineCourseRepository;
    @Mock private ClassroomEnrollmentRepository classroomEnrollmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private OnlineCourseService onlineCourseService;
    @Mock private ClassroomOfferingService classroomOfferingService;
    @Mock private PaymentReceiptPdfService paymentReceiptPdfService;

    @InjectMocks
    private PaymentServiceImpl service;

    @Test
    void createPayosPaymentLink_WithFee() throws Exception {
        String email = "learner@example.com";

        User learner = User.builder().id(1L).fullName("L1").email(email).phoneNumber("0900000000").build();
        LearningPackage pkg = LearningPackage.builder()
                .id(10L).title("IELTS").slug("ielts").price(BigDecimal.valueOf(500_000))
                .status(PackageStatus.PUBLISHED).deleted(false).build();
        OnlineCourse course = OnlineCourse.builder().id(100L).learningPackage(pkg).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(learner));
        when(onlineCourseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(List.of());

        when(payosProperties.isEnabled()).thenReturn(true);
        when(payosProperties.getClientId()).thenReturn("c");
        when(payosProperties.getApiKey()).thenReturn("a");
        when(payosProperties.getChecksumKey()).thenReturn("k");
        when(payosProperties.getCancelUrl()).thenReturn("u");
        when(payosProperties.getReturnUrl()).thenReturn("u");

        when(paymentOrderRepository.findByOrderCode(any(Long.class))).thenReturn(Optional.empty());
        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var paymentRequests = org.mockito.Mockito.mock(
                vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService.class);
        when(payOS.paymentRequests()).thenReturn(paymentRequests);
        var payosResponse = org.mockito.Mockito.mock(
                vn.payos.model.v2.paymentRequests.CreatePaymentLinkResponse.class);
        when(payosResponse.getPaymentLinkId()).thenReturn("link-123");
        when(payosResponse.getCheckoutUrl()).thenReturn("https://pay.payos.vn/web/123");
        when(payosResponse.getQrCode()).thenReturn("qr-123");
        when(paymentRequests.create(any())).thenReturn(payosResponse);
        when(payOS.getCrypto()).thenReturn(org.mockito.Mockito.mock(vn.payos.crypto.CryptoProvider.class));

        PaymentLinkResponse response = service.createPaymentLink(List.of(100L), List.of(), null, email);

        assertThat(response.getCheckoutUrl()).isEqualTo("https://pay.payos.vn/web/123");
        assertThat(response.getPaymentLinkId()).isEqualTo("link-123");
        assertThat(response.getStatus()).isEqualTo("PENDING");
        assertThat(response.getTotalAmount()).isEqualTo(500_000L);
    }

    @Test
    void handleWebhook_Success() throws Exception {
        long orderCode = 1700000000001L;
        String email = "learner@example.com";

        User learner = User.builder().id(1L).email(email).build();
        PaymentOrder pendingOrder = PaymentOrder.builder()
                .orderCode(orderCode).student(learner).courseIdsCsv("100")
                .status(PaymentOrderStatus.PENDING).build();
        assertThat(pendingOrder.getStatus()).isEqualTo(PaymentOrderStatus.PENDING);

        when(payosProperties.isEnabled()).thenReturn(true);
        when(payosProperties.getClientId()).thenReturn("c");
        when(payosProperties.getApiKey()).thenReturn("a");
        when(payosProperties.getChecksumKey()).thenReturn("k");
        when(paymentOrderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(pendingOrder));

        var crypto = org.mockito.Mockito.mock(vn.payos.crypto.CryptoProvider.class);
        when(payOS.getCrypto()).thenReturn(crypto);
        when(crypto.createSignatureFromObj(any(), any())).thenReturn("sig");

        Map<String, Object> payload = Map.of(
                "success", Boolean.TRUE, "code", "00", "signature", "sig",
                "data", Map.of("orderCode", orderCode, "amount", 500_000L,
                        "reference", "TXN123", "transaction", "TXN123",
                        "paymentLinkId", "link-123", "code", "00"));

        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handlePayosWebhook(payload);

        ArgumentCaptor<PaymentOrder> captor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(paymentOrderRepository, times(1)).save(captor.capture());
        PaymentOrder saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(PaymentOrderStatus.PAID);
        assertThat(saved.getProviderReference()).isEqualTo("TXN123");
        verify(onlineCourseService, times(1)).activatePaidCourse(100L, email);
        verify(classroomOfferingService, never()).applyPayosTuitionPayment(any(), any(), any());
    }

    @Test
    void createPaymentLink_ZeroTotal_SkipsPayosGateway() {
        String email = "learner@example.com";

        User learner = User.builder().id(1L).email(email).build();
        LearningPackage pkg = LearningPackage.builder()
                .id(10L).title("FreeCourse").slug("free").price(BigDecimal.ZERO)
                .status(PackageStatus.PUBLISHED).deleted(false).build();
        OnlineCourse course = OnlineCourse.builder().id(100L).learningPackage(pkg).build();
        DiscountCode coupon = DiscountCode.builder()
                .id(1L).code("FREE100").name("Free").type(DiscountType.PERCENTAGE)
                .value(BigDecimal.valueOf(100)).usageLimit(100).usedCount(0).reservedCount(0)
                .active(true).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(learner));
        when(onlineCourseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(List.of());
        when(discountCodeRepository.findByCodeIgnoreCase("FREE100"))
                .thenReturn(Optional.of(coupon));
        when(discountCodeRepository.findByCodeIgnoreCaseForUpdate("FREE100"))
                .thenReturn(Optional.of(coupon));

        PaymentLinkResponse response = service.createPaymentLink(List.of(100L), List.of(), "FREE100", email);

        assertThat(response.getStatus()).isEqualTo("PAID");
        assertThat(response.getTotalAmount()).isEqualTo(0L);
        assertThat(response.getCheckoutUrl()).isNull();
        assertThat(response.getOrderCode()).isNull();

        verify(payOS, never()).paymentRequests();
        verify(classroomOfferingService, never()).applyPayosTuitionPayment(any(), any(), any());

        verify(onlineCourseService, times(1)).activatePaidCourse(100L, email);
    }

    @Test
    void createPaymentLink_ExpiredCoupon() {
        String email = "learner@example.com";

        User learner = User.builder().id(1L).email(email).build();
        LearningPackage pkg = LearningPackage.builder()
                .id(10L).title("IELTS").slug("ielts").price(BigDecimal.valueOf(500_000))
                .status(PackageStatus.PUBLISHED).deleted(false).build();
        OnlineCourse course = OnlineCourse.builder().id(100L).learningPackage(pkg).build();
        DiscountCode expiredCoupon = DiscountCode.builder()
                .id(1L).code("EXPIRED2026").name("X").type(DiscountType.PERCENTAGE)
                .value(BigDecimal.valueOf(50)).usageLimit(100).usedCount(0).reservedCount(0)
                .active(true).startsAt(LocalDateTime.now().minusDays(30))
                .expiresAt(LocalDateTime.now().minusDays(1))
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(learner));
        when(onlineCourseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(List.of());
        when(discountCodeRepository.findByCodeIgnoreCase("EXPIRED2026"))
                .thenReturn(Optional.of(expiredCoupon));

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.createPaymentLink(List.of(100L), List.of(), "EXPIRED2026", email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("hß║┐t hß║ín");

        verify(payOS, never()).paymentRequests();
        verify(onlineCourseService, never()).activatePaidCourse(any(Long.class), anyString());
        verify(paymentOrderRepository, never()).save(any());
    }

    @Test
    void createPaymentLink_AlreadyEnrolled() throws Exception {
        String email = "learner@example.com";
        User learner = User.builder().id(1L).email(email).build();
        LearningPackage pkg = LearningPackage.builder()
                .id(10L).title("IELTS").slug("ielts").price(BigDecimal.valueOf(500_000))
                .status(PackageStatus.PUBLISHED).deleted(false).build();
        OnlineCourse course = OnlineCourse.builder().id(10L).learningPackage(pkg).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(learner));
        when(onlineCourseRepository.findById(10L)).thenReturn(Optional.of(course));

        when(onlineCourseService.getMyEnrollments(email))
                .thenReturn(List.of(fu.sep490.g23.backend.dto.response.course.PackageEnrollmentResponse
                        .builder().id(1L).courseId(10L).courseTitle("IELTS").build()));

        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.createPaymentLink(List.of(10L), List.of(), null, email))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("─æ─âng k├╜");

        verify(payOS, never()).paymentRequests();
        verify(paymentOrderRepository, never()).save(any());
        verify(onlineCourseService, never()).activatePaidCourse(any(Long.class), anyString());
    }

    @Test
    void handleWebhook_FailedStatus() {
        long orderCode = 1700000000002L;
        String email = "learner@example.com";

        User learner = User.builder().id(1L).email(email).build();
        PaymentOrder pendingOrder = PaymentOrder.builder()
                .orderCode(orderCode).student(learner).courseIdsCsv("100")
                .status(PaymentOrderStatus.PENDING).build();

        when(payosProperties.isEnabled()).thenReturn(true);
        when(payosProperties.getClientId()).thenReturn("c");
        when(payosProperties.getApiKey()).thenReturn("a");
        when(payosProperties.getChecksumKey()).thenReturn("k");
        when(paymentOrderRepository.findByOrderCode(orderCode)).thenReturn(Optional.of(pendingOrder));

        var crypto = org.mockito.Mockito.mock(vn.payos.crypto.CryptoProvider.class);
        when(payOS.getCrypto()).thenReturn(crypto);
        when(crypto.createSignatureFromObj(any(), any())).thenReturn("sig");

        Map<String, Object> payload = Map.of(
                "success", Boolean.FALSE,
                "code", "01",
                "signature", "sig",
                "desc", "Thanh to├ín thß║Ñt bß║íi",
                "data", Map.of(
                        "orderCode", orderCode,
                        "amount", 500_000L,
                        "reference", "TXN123",
                        "transaction", "TXN123",
                        "paymentLinkId", "link-123",
                        "code", "01",
                        "desc", "Giao dß╗ïch kh├┤ng th├ánh c├┤ng"));

        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.handlePayosWebhook(payload);

        ArgumentCaptor<PaymentOrder> captor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(paymentOrderRepository, times(1)).save(captor.capture());
        PaymentOrder saved = captor.getValue();

        assertThat(saved.getStatus()).isEqualTo(PaymentOrderStatus.FAILED);
        assertThat(saved.getProviderReference()).isEqualTo("TXN123");

        verify(onlineCourseService, never()).activatePaidCourse(any(Long.class), anyString());
        verify(classroomOfferingService, never()).applyPayosTuitionPayment(any(), any(), any());
    }

    @Test
    void createPaymentLink_SocketTimeout() {

        String email = "learner@example.com";
        User learner = User.builder().id(1L).email(email).build();
        LearningPackage pkg = LearningPackage.builder()
                .id(10L).title("IELTS").slug("ielts").price(BigDecimal.valueOf(500_000))
                .status(PackageStatus.PUBLISHED).deleted(false).build();
        OnlineCourse course = OnlineCourse.builder().id(100L).learningPackage(pkg).build();
        PaymentOrder savedOrder = PaymentOrder.builder()
                .orderCode(1700000000099L).student(learner).courseIdsCsv("100")
                .originalAmount(500_000L).amount(500_000L).status(PaymentOrderStatus.PENDING).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(learner));
        when(onlineCourseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(List.of());
        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(payosProperties.isEnabled()).thenReturn(true);
        when(payosProperties.getClientId()).thenReturn("c");
        when(payosProperties.getApiKey()).thenReturn("a");
        when(payosProperties.getChecksumKey()).thenReturn("k");
        when(payosProperties.getCancelUrl()).thenReturn("u");
        when(payosProperties.getReturnUrl()).thenReturn("u");

        var paymentRequests = org.mockito.Mockito.mock(
                vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService.class);
        when(payOS.paymentRequests()).thenReturn(paymentRequests);
        var crypto = org.mockito.Mockito.mock(vn.payos.crypto.CryptoProvider.class);
        when(crypto.createSignatureOfPaymentRequest(any(), any())).thenReturn("mock-signature");
        when(payOS.getCrypto()).thenReturn(crypto);
        when(paymentRequests.create(any())).thenThrow(new RuntimeException(
                new java.net.SocketTimeoutException("Read timed out")));


        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.createPaymentLink(List.of(100L), List.of(), null, email))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unable to connect to the payment gateway. Please try again later.");


        ArgumentCaptor<PaymentOrder> captor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(paymentOrderRepository, atLeast(1)).save(captor.capture());
        PaymentOrder persisted = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(persisted.getStatus()).isEqualTo(PaymentOrderStatus.FAILED);
    }

    @Test
    void createPaymentLink_Http500() {

        String email = "learner@example.com";
        User learner = User.builder().id(1L).email(email).build();
        LearningPackage pkg = LearningPackage.builder()
                .id(10L).title("IELTS").slug("ielts").price(BigDecimal.valueOf(500_000))
                .status(PackageStatus.PUBLISHED).deleted(false).build();
        OnlineCourse course = OnlineCourse.builder().id(100L).learningPackage(pkg).build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(learner));
        when(onlineCourseRepository.findById(100L)).thenReturn(Optional.of(course));
        when(onlineCourseService.getMyEnrollments(email)).thenReturn(List.of());
        when(paymentOrderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(payosProperties.isEnabled()).thenReturn(true);
        when(payosProperties.getClientId()).thenReturn("c");
        when(payosProperties.getApiKey()).thenReturn("a");
        when(payosProperties.getChecksumKey()).thenReturn("k");
        when(payosProperties.getCancelUrl()).thenReturn("u");
        when(payosProperties.getReturnUrl()).thenReturn("u");
        var paymentRequests = org.mockito.Mockito.mock(
                vn.payos.service.blocking.v2.paymentRequests.PaymentRequestsService.class);
        when(payOS.paymentRequests()).thenReturn(paymentRequests);
        var crypto = org.mockito.Mockito.mock(vn.payos.crypto.CryptoProvider.class);
        when(crypto.createSignatureOfPaymentRequest(any(), any())).thenReturn("mock-signature");
        when(payOS.getCrypto()).thenReturn(crypto);
        when(paymentRequests.create(any())).thenThrow(new org.springframework.web.client.HttpServerErrorException(
                org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error"));


        org.assertj.core.api.Assertions
                .assertThatThrownBy(() -> service.createPaymentLink(List.of(100L), List.of(), null, email))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Unable to connect to the payment gateway. Please try again later.");

        ArgumentCaptor<PaymentOrder> captor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(paymentOrderRepository, atLeast(1)).save(captor.capture());
        PaymentOrder persisted = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(persisted.getStatus()).isEqualTo(PaymentOrderStatus.FAILED);
    }
}
