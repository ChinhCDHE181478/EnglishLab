package fu.sap490.g23.backend.service.payment;

import fu.sap490.g23.backend.dto.response.payment.PaymentLinkResponse;
import fu.sap490.g23.backend.dto.response.payment.PaymentOrderStatusResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageStatus;
import fu.sap490.g23.backend.entity.payment.PaymentOrder;
import fu.sap490.g23.backend.entity.payment.PaymentOrderStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sap490.g23.backend.service.course.OnlineCourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PayosProperties payosProperties;
    private final PaymentOrderRepository paymentOrderRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final UserRepository userRepository;
    private final OnlineCourseService onlineCourseService;
    @Override
    public PaymentLinkResponse createPaymentLink(List<Long> courseIds, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người học."));

        List<OnlineCourse> courses = resolvePayableCourses(courseIds, student);
        long amount = courses.stream()
                .map(OnlineCourse::getLearningPackage)
                .map(item -> item.getPrice() == null ? BigDecimal.ZERO : item.getPrice())
                .mapToLong(BigDecimal::longValue)
                .sum();

        if (amount <= 0) {
            courses.forEach(course -> onlineCourseService.registerCourse(course.getId(), studentEmail));
            return PaymentLinkResponse.builder()
                    .orderCode(null)
                    .paymentLinkId(null)
                    .checkoutUrl(null)
                    .qrCode(null)
                    .status(PaymentOrderStatus.PAID.name())
                    .build();
        }

        ensurePayosEnabled();

        long orderCode = buildOrderCode();
        String orderCodeText = String.valueOf(orderCode);
        String description = "ELAB" + orderCodeText.substring(Math.max(0, orderCodeText.length() - 6));
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .orderCode(orderCode)
                .student(student)
                .courseIdsCsv(courses.stream().map(course -> String.valueOf(course.getId())).collect(Collectors.joining(",")))
                .courseTitles(courses.stream().map(course -> course.getLearningPackage().getTitle()).collect(Collectors.joining(" | ")))
                .amount(amount)
                .description(description)
                .status(PaymentOrderStatus.PENDING)
                .build();
        paymentOrderRepository.save(paymentOrder);

        try {
            PayOS client = createClient();
            List<PaymentLinkItem> items = courses.stream()
                    .map(course -> PaymentLinkItem.builder()
                            .name(course.getLearningPackage().getTitle())
                            .quantity(1)
                            .price(resolveCoursePrice(course))
                            .unit("khóa học")
                            .build())
                    .toList();

            var request = vn.payos.model.v2.paymentRequests.CreatePaymentLinkRequest.builder()
                    .orderCode(orderCode)
                    .amount(amount)
                    .description(description)
                    .cancelUrl(payosProperties.getCancelUrl())
                    .returnUrl(payosProperties.getReturnUrl())
                    .items(items)
                    .buyerName(student.getFullName())
                    .buyerEmail(student.getEmail())
                    .buyerPhone(student.getPhoneNumber())
                    .build();
            request.setSignature(client.getCrypto().createSignatureOfPaymentRequest(request, payosProperties.getChecksumKey()));

            var response = client.paymentRequests().create(request);
            paymentOrder.setPaymentLinkId(response.getPaymentLinkId());
            paymentOrder.setCheckoutUrl(response.getCheckoutUrl());
            paymentOrderRepository.save(paymentOrder);

            return PaymentLinkResponse.builder()
                    .orderCode(paymentOrder.getOrderCode())
                    .paymentLinkId(response.getPaymentLinkId())
                    .checkoutUrl(response.getCheckoutUrl())
                    .qrCode(response.getQrCode())
                    .status(paymentOrder.getStatus().name())
                    .build();
        } catch (PayOSException ex) {
            paymentOrder.setStatus(PaymentOrderStatus.FAILED);
            paymentOrderRepository.save(paymentOrder);
            throw new RuntimeException("Không tạo được link thanh toán PayOS: " + ex.getMessage(), ex);
        }
    }

    @Override
    public PaymentOrderStatusResponse getOrderStatus(Long orderCode, String studentEmail) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn thanh toán."));

        if (!Objects.equals(order.getStudent().getEmail(), studentEmail)) {
            throw new RuntimeException("Bạn không có quyền xem đơn thanh toán này.");
        }

        if (payosProperties.isEnabled() && isPendingStatus(order.getStatus())) {
            syncOrderStatusFromProvider(order);
        }

        return PaymentOrderStatusResponse.builder()
                .orderCode(order.getOrderCode())
                .status(order.getStatus().name())
                .paid(order.getStatus() == PaymentOrderStatus.PAID)
                .message(resolveStatusMessage(order.getStatus()))
                .build();
    }

    @Override
    public void handlePayosWebhook(Map<String, Object> payload) {
        ensurePayosEnabled();

        Map<String, Object> data = castMap(payload.get("data"));
        String signature = String.valueOf(payload.getOrDefault("signature", ""));
        if (data.isEmpty() || signature.isBlank()) {
            throw new RuntimeException("Webhook PayOS không hợp lệ.");
        }

        try {
            PayOS client = createClient();
            String expectedSignature = client.getCrypto().createSignatureFromObj(data, payosProperties.getChecksumKey());
            if (!expectedSignature.equals(signature)) {
                throw new RuntimeException("Chữ ký webhook PayOS không hợp lệ.");
            }
        } catch (PayOSException ex) {
            throw new RuntimeException("Không xác thực được webhook PayOS: " + ex.getMessage(), ex);
        }

        Long orderCode = toLong(data.get("orderCode"));
        if (orderCode == null) {
            return;
        }

        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode).orElse(null);
        if (order == null) {
            log.info("Bỏ qua webhook PayOS cho orderCode={} vì không có trong hệ thống.", orderCode);
            return;
        }

        order.setProviderReference(stringValue(data.get("reference")));
        order.setPaymentLinkId(firstNonBlank(order.getPaymentLinkId(), stringValue(data.get("paymentLinkId"))));
        order.setWebhookConfirmedAt(LocalDateTime.now());
        order.setLastWebhookPayload(writePayload(payload));

        boolean success = Boolean.TRUE.equals(payload.get("success"))
                && "00".equalsIgnoreCase(stringValue(payload.get("code")))
                && "00".equalsIgnoreCase(stringValue(data.get("code")));

        if (success) {
            if (order.getStatus() != PaymentOrderStatus.PAID) {
                order.setStatus(PaymentOrderStatus.PAID);
                order.setPaidAt(LocalDateTime.now());
                enrollPurchasedCourses(order);
            }
        } else {
            order.setStatus(resolveFailureStatus(payload, data));
        }

        paymentOrderRepository.save(order);
    }

    @Override
    public void confirmWebhook() {
        ensurePayosEnabled();
        if (payosProperties.getWebhookUrl() == null || payosProperties.getWebhookUrl().isBlank()) {
            throw new RuntimeException("Chưa cấu hình webhook URL cho PayOS.");
        }

        try {
            createClient().webhooks().confirm(payosProperties.getWebhookUrl());
        } catch (PayOSException ex) {
            throw new RuntimeException("Không xác nhận được webhook PayOS: " + ex.getMessage(), ex);
        }
    }

    private List<OnlineCourse> resolvePayableCourses(List<Long> courseIds, User student) {
        List<OnlineCourse> courses = new ArrayList<>();
        for (Long courseId : courseIds.stream().filter(Objects::nonNull).distinct().toList()) {
            OnlineCourse course = onlineCourseRepository.findById(courseId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khóa học có mã " + courseId + "."));
            if (course.getLearningPackage() == null
                    || course.getLearningPackage().isDeleted()
                    || course.getLearningPackage().getStatus() != PackageStatus.PUBLISHED) {
                throw new RuntimeException("Có khóa học hiện không còn khả dụng để thanh toán.");
            }

            boolean enrolled = onlineCourseService.getMyEnrollments(student.getEmail()).stream()
                    .anyMatch(item -> Objects.equals(item.getCourseId(), courseId));
            if (enrolled) {
                throw new RuntimeException("Có khóa học đã được đăng ký trước đó.");
            }
            courses.add(course);
        }

        if (courses.isEmpty()) {
            throw new RuntimeException("Không có khóa học hợp lệ để thanh toán.");
        }

        return courses.stream()
                .sorted(Comparator.comparing(course -> course.getLearningPackage().getTitle(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void enrollPurchasedCourses(PaymentOrder order) {
        for (Long courseId : parseCourseIds(order.getCourseIdsCsv())) {
            try {
                onlineCourseService.registerCourse(courseId, order.getStudent().getEmail());
            } catch (RuntimeException ex) {
                String normalizedMessage = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase(Locale.ROOT);
                if (!normalizedMessage.contains("already") && !normalizedMessage.contains("đã")) {
                    throw ex;
                }
            }
        }
    }

    private List<Long> parseCourseIds(String courseIdsCsv) {
        if (courseIdsCsv == null || courseIdsCsv.isBlank()) {
            return List.of();
        }
        return List.of(courseIdsCsv.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(Long::valueOf)
                .toList();
    }

    private PaymentOrderStatus resolveFailureStatus(Map<String, Object> payload, Map<String, Object> data) {
        String code = stringValue(data.get("code"));
        String desc = (stringValue(data.get("desc")) + " " + stringValue(payload.get("desc"))).toLowerCase(Locale.ROOT);
        if ("03".equals(code) || desc.contains("cancel") || desc.contains("hủy")) {
            return PaymentOrderStatus.CANCELLED;
        }
        if (desc.contains("expired") || desc.contains("hết hạn")) {
            return PaymentOrderStatus.EXPIRED;
        }
        return PaymentOrderStatus.FAILED;
    }

    private String resolveStatusMessage(PaymentOrderStatus status) {
        return switch (status) {
            case PAID -> "Thanh toán thành công. Khóa học đã được cập nhật vào tài khoản của bạn.";
            case PENDING, PROCESSING -> "Đơn thanh toán đang chờ PayOS xác nhận. Vui lòng tải lại sau vài giây.";
            case CANCELLED -> "Đơn thanh toán đã bị hủy.";
            case EXPIRED -> "Link thanh toán đã hết hạn.";
            case FAILED -> "Thanh toán chưa thành công. Vui lòng thử lại.";
        };
    }

    private void syncOrderStatusFromProvider(PaymentOrder order) {
        try {
            var paymentLink = createClient().paymentRequests().get(order.getOrderCode());
            order.setPaymentLinkId(firstNonBlank(order.getPaymentLinkId(), paymentLink.getId()));

            PaymentOrderStatus nextStatus = mapProviderStatus(paymentLink.getStatus());
            if (nextStatus == PaymentOrderStatus.PAID && order.getStatus() != PaymentOrderStatus.PAID) {
                order.setStatus(PaymentOrderStatus.PAID);
                order.setPaidAt(firstNonNull(order.getPaidAt(), LocalDateTime.now()));
                enrollPurchasedCourses(order);
            } else if (nextStatus != order.getStatus()) {
                order.setStatus(nextStatus);
            }

            paymentOrderRepository.save(order);
        } catch (PayOSException ex) {
            log.warn("Không thể đồng bộ trạng thái PayOS cho orderCode={}: {}", order.getOrderCode(), ex.getMessage());
        }
    }

    private PaymentOrderStatus mapProviderStatus(PaymentLinkStatus status) {
        if (status == null) {
            return PaymentOrderStatus.PENDING;
        }

        return switch (status) {
            case PAID -> PaymentOrderStatus.PAID;
            case PROCESSING, UNDERPAID -> PaymentOrderStatus.PROCESSING;
            case CANCELLED -> PaymentOrderStatus.CANCELLED;
            case EXPIRED -> PaymentOrderStatus.EXPIRED;
            case FAILED -> PaymentOrderStatus.FAILED;
            case PENDING -> PaymentOrderStatus.PENDING;
        };
    }

    private boolean isPendingStatus(PaymentOrderStatus status) {
        return status == PaymentOrderStatus.PENDING || status == PaymentOrderStatus.PROCESSING;
    }

    private PayOS createClient() {
        return new PayOS(
                vn.payos.core.ClientOptions.builder()
                        .clientId(payosProperties.getClientId())
                        .apiKey(payosProperties.getApiKey())
                        .checksumKey(payosProperties.getChecksumKey())
                        .build()
        );
    }

    private void ensurePayosEnabled() {
        if (!payosProperties.isEnabled()) {
            throw new RuntimeException("Tích hợp PayOS hiện đang tắt.");
        }
        if (isBlank(payosProperties.getClientId()) || isBlank(payosProperties.getApiKey()) || isBlank(payosProperties.getChecksumKey())) {
            throw new RuntimeException("Thiếu cấu hình PayOS trong backend.");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private long resolveCoursePrice(OnlineCourse course) {
        if (course.getLearningPackage() == null || course.getLearningPackage().getPrice() == null) {
            return 0L;
        }
        return course.getLearningPackage().getPrice().longValue();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private LocalDateTime firstNonNull(LocalDateTime first, LocalDateTime second) {
        return first != null ? first : second;
    }

    private String writePayload(Map<String, Object> payload) {
        return payload == null ? "{}" : payload.toString();
    }

    private long buildOrderCode() {
        long orderCode = System.currentTimeMillis();
        while (paymentOrderRepository.findByOrderCode(orderCode).isPresent()) {
            orderCode += 1;
        }
        return orderCode;
    }
}
