package fu.sap490.g23.backend.service.payment.impl;

import fu.sap490.g23.backend.service.payment.*;


import fu.sap490.g23.backend.dto.response.payment.PaymentLinkResponse;
import fu.sap490.g23.backend.dto.response.payment.PaymentOrderStatusResponse;
import fu.sap490.g23.backend.dto.response.payment.PaymentOrderSummaryResponse;
import fu.sap490.g23.backend.dto.response.payment.PaymentQuoteResponse;
import fu.sap490.g23.backend.dto.response.payment.RevenueAnalyticsResponse;
import fu.sap490.g23.backend.dto.response.payment.RevenueByMonthResponse;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.payment.DiscountCode;
import fu.sap490.g23.backend.entity.payment.enums.DiscountType;
import fu.sap490.g23.backend.entity.payment.PaymentOrder;
import fu.sap490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.payment.DiscountCodeRepository;
import fu.sap490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sap490.g23.backend.service.course.OnlineCourseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import vn.payos.PayOS;
import vn.payos.exception.PayOSException;
import vn.payos.model.v2.paymentRequests.PaymentLinkItem;
import vn.payos.model.v2.paymentRequests.PaymentLinkStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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
    private final DiscountCodeRepository discountCodeRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final UserRepository userRepository;
    private final OnlineCourseService onlineCourseService;

    @Override
    @Transactional(readOnly = true)
    public PaymentQuoteResponse quotePayment(List<Long> courseIds, List<Long> classroomOfferingIds, String couponCode, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người học."));
        PayableBundle bundle = resolvePayableBundle(courseIds, classroomOfferingIds, student);
        return toQuoteResponse(calculateBreakdown(bundle, couponCode, false));
    }

    @Override
    public PaymentLinkResponse createPaymentLink(List<Long> courseIds, List<Long> classroomOfferingIds, String couponCode, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người học."));
        PayableBundle bundle = resolvePayableBundle(courseIds, classroomOfferingIds, student);
        PriceBreakdown previewBreakdown = calculateBreakdown(bundle, couponCode, false);

        if (previewBreakdown.totalAmount() <= 0) {
            PriceBreakdown breakdown = calculateBreakdown(bundle, couponCode, true);
            enrollPurchasedItems(bundle, studentEmail);
            consumeCouponReservation(breakdown.discountCode());
            return PaymentLinkResponse.builder()
                    .orderCode(null)
                    .paymentLinkId(null)
                    .checkoutUrl(null)
                    .qrCode(null)
                    .status(PaymentOrderStatus.PAID.name())
                    .originalAmount(breakdown.originalAmount())
                    .systemDiscountAmount(breakdown.systemDiscountAmount())
                    .couponDiscountAmount(breakdown.couponDiscountAmount())
                    .totalAmount(0L)
                    .couponCode(breakdown.couponCode())
                    .build();
        }

        ensurePayosEnabled();
        PriceBreakdown breakdown = calculateBreakdown(bundle, couponCode, true);
        long amount = breakdown.totalAmount();

        long orderCode = buildOrderCode();
        String orderCodeText = String.valueOf(orderCode);
        String description = "ELAB" + orderCodeText.substring(Math.max(0, orderCodeText.length() - 6));
        PaymentOrder paymentOrder = PaymentOrder.builder()
                .orderCode(orderCode)
                .student(student)
                .courseIdsCsv(bundle.onlineCourses().stream().map(course -> String.valueOf(course.getId())).collect(Collectors.joining(",")))
                .classroomOfferingIdsCsv("")
                .courseTitles(bundle.allTitles())
                .amount(amount)
                .originalAmount(breakdown.originalAmount())
                .systemDiscountAmount(breakdown.systemDiscountAmount())
                .couponDiscountAmount(breakdown.couponDiscountAmount())
                .discountCode(breakdown.discountCode())
                .discountCodeText(breakdown.couponCode())
                .couponReservationReleased(breakdown.discountCode() == null)
                .description(description)
                .status(PaymentOrderStatus.PENDING)
                .build();
        paymentOrderRepository.save(paymentOrder);

        try {
            PayOS client = createClient();
            List<PaymentLinkItem> items = List.of(PaymentLinkItem.builder()
                    .name("EnglishLab order")
                    .quantity(1)
                    .price(amount)
                    .unit("order")
                    .build());

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
                    .originalAmount(paymentOrder.getOriginalAmount())
                    .systemDiscountAmount(paymentOrder.getSystemDiscountAmount())
                    .couponDiscountAmount(paymentOrder.getCouponDiscountAmount())
                    .totalAmount(paymentOrder.getAmount())
                    .couponCode(paymentOrder.getDiscountCodeText())
                    .build();
        } catch (PayOSException ex) {
            paymentOrder.setStatus(PaymentOrderStatus.FAILED);
            releaseCouponReservation(paymentOrder);
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
            markOrderPaid(order);
        } else {
            order.setStatus(resolveFailureStatus(payload, data));
            if (isFinalFailure(order.getStatus())) {
                releaseCouponReservation(order);
            }
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

    @Scheduled(fixedDelayString = "${englishlab.payos.reconciliation-delay-ms:300000}", initialDelayString = "${englishlab.payos.reconciliation-initial-delay-ms:60000}")
    @Transactional
    public void reconcilePendingPaymentOrders() {
        if (!payosProperties.isEnabled()) {
            return;
        }
        paymentOrderRepository.findByStatusIn(List.of(PaymentOrderStatus.PENDING, PaymentOrderStatus.PROCESSING))
                .forEach(this::syncOrderStatusFromProvider);
    }

    private PriceBreakdown calculateBreakdown(PayableBundle bundle, String couponCode, boolean reserveCoupon) {
        List<LearningPackage> packages = bundle.packages();
        long originalAmount = packages.stream().mapToLong(pkg -> toVnd(resolveOriginalPrice(pkg))).sum();
        long subtotalAmount = packages.stream().mapToLong(pkg -> toVnd(resolveSystemPrice(pkg))).sum();
        long systemDiscountAmount = Math.max(0L, originalAmount - subtotalAmount);
        DiscountCode discountCode = resolveDiscountCode(couponCode, reserveCoupon);
        long couponDiscountAmount = calculateCouponDiscount(discountCode, subtotalAmount);
        long totalAmount = Math.max(0L, subtotalAmount - couponDiscountAmount);
        return new PriceBreakdown(
                originalAmount,
                systemDiscountAmount,
                subtotalAmount,
                couponDiscountAmount,
                totalAmount,
                discountCode,
                discountCode == null ? null : discountCode.getCode(),
                discountCode == null ? null : "Mã giảm giá đã được áp dụng."
        );
    }

    private DiscountCode resolveDiscountCode(String couponCode, boolean reserveCoupon) {
        String normalizedCode = normalizeCouponCode(couponCode);
        if (normalizedCode == null) {
            return null;
        }

        DiscountCode discountCode = (reserveCoupon
                ? discountCodeRepository.findByCodeIgnoreCaseForUpdate(normalizedCode)
                : discountCodeRepository.findByCodeIgnoreCase(normalizedCode))
                .orElseThrow(() -> new RuntimeException("Mã giảm giá không tồn tại."));
        validateDiscountCode(discountCode);

        if (reserveCoupon) {
            discountCode.setReservedCount(safeCount(discountCode.getReservedCount()) + 1);
        }
        return discountCode;
    }

    private void validateDiscountCode(DiscountCode discountCode) {
        LocalDateTime now = LocalDateTime.now();
        if (!discountCode.isActive()) {
            throw new RuntimeException("Mã giảm giá hiện không hoạt động.");
        }
        if (discountCode.getStartsAt() != null && now.isBefore(discountCode.getStartsAt())) {
            throw new RuntimeException("Mã giảm giá chưa đến thời gian sử dụng.");
        }
        if (discountCode.getExpiresAt() != null && now.isAfter(discountCode.getExpiresAt())) {
            throw new RuntimeException("Mã giảm giá đã hết hạn.");
        }
        int usedCount = safeCount(discountCode.getUsedCount());
        int reservedCount = safeCount(discountCode.getReservedCount());
        int usageLimit = safeCount(discountCode.getUsageLimit());
        if (usageLimit <= 0 || usedCount + reservedCount >= usageLimit) {
            throw new RuntimeException("Mã giảm giá đã hết lượt sử dụng.");
        }
    }

    private long calculateCouponDiscount(DiscountCode discountCode, long subtotalAmount) {
        if (discountCode == null || subtotalAmount <= 0) {
            return 0L;
        }
        BigDecimal subtotal = BigDecimal.valueOf(subtotalAmount);
        BigDecimal discount = discountCode.getType() == DiscountType.PERCENTAGE
                ? subtotal.multiply(discountCode.getValue()).divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                : discountCode.getValue();
        return Math.min(subtotalAmount, Math.max(0L, toVnd(discount)));
    }

    private PaymentQuoteResponse toQuoteResponse(PriceBreakdown breakdown) {
        return PaymentQuoteResponse.builder()
                .originalAmount(breakdown.originalAmount())
                .systemDiscountAmount(breakdown.systemDiscountAmount())
                .subtotalAmount(breakdown.subtotalAmount())
                .couponDiscountAmount(breakdown.couponDiscountAmount())
                .totalAmount(breakdown.totalAmount())
                .couponCode(breakdown.couponCode())
                .couponMessage(breakdown.couponMessage())
                .build();
    }

    private void markOrderPaid(PaymentOrder order) {
        if (order.getStatus() != PaymentOrderStatus.PAID) {
            order.setStatus(PaymentOrderStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            consumeCouponReservation(order);
            enrollPurchasedCourses(order);
        }
    }

    private void consumeCouponReservation(PaymentOrder order) {
        if (order.getDiscountCode() == null || order.isCouponReservationReleased()) {
            return;
        }
        consumeCouponReservation(order.getDiscountCode());
        order.setCouponReservationReleased(true);
    }

    private void consumeCouponReservation(DiscountCode discountCode) {
        if (discountCode == null) {
            return;
        }
        discountCode.setReservedCount(Math.max(0, safeCount(discountCode.getReservedCount()) - 1));
        discountCode.setUsedCount(safeCount(discountCode.getUsedCount()) + 1);
    }

    private void releaseCouponReservation(PaymentOrder order) {
        if (order.getDiscountCode() == null || order.isCouponReservationReleased()) {
            return;
        }
        DiscountCode discountCode = order.getDiscountCode();
        discountCode.setReservedCount(Math.max(0, safeCount(discountCode.getReservedCount()) - 1));
        order.setCouponReservationReleased(true);
    }

    private PayableBundle resolvePayableBundle(List<Long> courseIds, List<Long> classroomOfferingIds, User student) {
        List<Long> normalizedCourseIds = normalizeIds(courseIds);
        List<Long> normalizedClassroomIds = normalizeIds(classroomOfferingIds);
        if (normalizedClassroomIds.isEmpty() && normalizedCourseIds.isEmpty()) {
            throw new RuntimeException("Không có khóa học hợp lệ để thanh toán.");
        }
        if (!normalizedClassroomIds.isEmpty()) {
            throw new RuntimeException(
                    "Lớp Offline/Virtual không thanh toán qua giỏ hàng. Vui lòng đăng ký lớp và nộp học phí theo luồng đăng ký."
            );
        }
        List<OnlineCourse> courses = resolvePayableCourses(normalizedCourseIds, student);
        return new PayableBundle(courses);
    }

    private List<Long> normalizeIds(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().toList();
    }

    private List<OnlineCourse> resolvePayableCourses(List<Long> courseIds, User student) {
        List<OnlineCourse> courses = new ArrayList<>();
        for (Long courseId : courseIds) {
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

        return courses.stream()
                .sorted(Comparator.comparing(course -> course.getLearningPackage().getTitle(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private void enrollPurchasedItems(PayableBundle bundle, String studentEmail) {
        bundle.onlineCourses().forEach(course -> onlineCourseService.activatePaidCourse(course.getId(), studentEmail));
    }

    private void enrollPurchasedCourses(PaymentOrder order) {
        for (Long courseId : parseCourseIds(order.getCourseIdsCsv())) {
            try {
                onlineCourseService.activatePaidCourse(courseId, order.getStudent().getEmail());
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
            if (nextStatus == PaymentOrderStatus.PAID) {
                markOrderPaid(order);
            } else if (nextStatus != order.getStatus()) {
                order.setStatus(nextStatus);
                if (isFinalFailure(nextStatus)) {
                    releaseCouponReservation(order);
                }
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

    private boolean isFinalFailure(PaymentOrderStatus status) {
        return status == PaymentOrderStatus.CANCELLED
                || status == PaymentOrderStatus.EXPIRED
                || status == PaymentOrderStatus.FAILED;
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

    private BigDecimal resolveOriginalPrice(LearningPackage learningPackage) {
        return learningPackage == null || learningPackage.getPrice() == null ? BigDecimal.ZERO : learningPackage.getPrice();

    }

    private BigDecimal resolveSystemPrice(LearningPackage learningPackage) {
        BigDecimal originalPrice = resolveOriginalPrice(learningPackage);

        BigDecimal salePrice = learningPackage == null ? null : learningPackage.getSalePrice();

        if (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) < 0 || salePrice.compareTo(originalPrice) >= 0) {
            return originalPrice;
        }
        return salePrice;
    }

    private BigDecimal resolveOriginalPrice(OnlineCourse course) {
        return resolveOriginalPrice(course.getLearningPackage());
    }

    private BigDecimal resolveSystemPrice(OnlineCourse course) {
        return resolveSystemPrice(course.getLearningPackage());
    }

    private long toVnd(BigDecimal value) {
        if (value == null) {
            return 0L;
        }
        return value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private int safeCount(Integer value) {
        return Math.max(0, value == null ? 0 : value);
    }

    private String normalizeCouponCode(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return null;
        }
        return couponCode.trim().replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
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

    @Override
    @Transactional(readOnly = true)
    public List<PaymentOrderSummaryResponse> listMyOrders(String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người học."));
        return paymentOrderRepository.findByStudentOrderByCreatedAtDesc(student).stream()
                .map(this::toOrderSummary)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueAnalyticsResponse getRevenueAnalytics() {
        List<PaymentOrder> orders = paymentOrderRepository.findAll();
        long paid = orders.stream().filter(order -> order.getStatus() == PaymentOrderStatus.PAID).count();
        long failed = orders.stream().filter(order -> order.getStatus() == PaymentOrderStatus.FAILED
                || order.getStatus() == PaymentOrderStatus.CANCELLED
                || order.getStatus() == PaymentOrderStatus.EXPIRED).count();
        long pending = orders.stream().filter(order -> order.getStatus() == PaymentOrderStatus.PENDING
                || order.getStatus() == PaymentOrderStatus.PROCESSING).count();
        long totalRevenue = orders.stream()
                .filter(order -> order.getStatus() == PaymentOrderStatus.PAID)
                .mapToLong(order -> order.getAmount() == null ? 0L : order.getAmount())
                .sum();
        long totalDiscount = orders.stream()
                .filter(order -> order.getStatus() == PaymentOrderStatus.PAID)
                .mapToLong(order -> safeLong(order.getSystemDiscountAmount()) + safeLong(order.getCouponDiscountAmount()))
                .sum();
        long couponDiscount = orders.stream()
                .filter(order -> order.getStatus() == PaymentOrderStatus.PAID)
                .mapToLong(order -> safeLong(order.getCouponDiscountAmount()))
                .sum();

        Map<YearMonth, List<PaymentOrder>> byMonth = orders.stream()
                .filter(order -> order.getStatus() == PaymentOrderStatus.PAID && order.getPaidAt() != null)
                .collect(Collectors.groupingBy(order -> YearMonth.from(order.getPaidAt())));
        List<RevenueByMonthResponse> monthly = byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> RevenueByMonthResponse.builder()
                        .month(entry.getKey().format(DateTimeFormatter.ofPattern("yyyy-MM")))
                        .revenueVnd(entry.getValue().stream().mapToLong(order -> safeLong(order.getAmount())).sum())
                        .orderCount(entry.getValue().size())
                        .build())
                .toList();

        return RevenueAnalyticsResponse.builder()
                .totalOrders(orders.size())
                .paidOrders(paid)
                .failedOrders(failed)
                .pendingOrders(pending)
                .totalRevenueVnd(totalRevenue)
                .totalDiscountVnd(totalDiscount)
                .totalCouponDiscountVnd(couponDiscount)
                .monthlyRevenue(monthly)
                .build();
    }

    private PaymentOrderSummaryResponse toOrderSummary(PaymentOrder order) {
        List<String> titles = order.getCourseTitles() == null || order.getCourseTitles().isBlank()
                ? List.of()
                : List.of(order.getCourseTitles().split("\\|"));
        return PaymentOrderSummaryResponse.builder()
                .orderCode(order.getOrderCode())
                .status(order.getStatus().name())
                .paid(order.getStatus() == PaymentOrderStatus.PAID)
                .amount(safeLong(order.getAmount()))
                .originalAmount(safeLong(order.getOriginalAmount()))
                .systemDiscountAmount(safeLong(order.getSystemDiscountAmount()))
                .couponDiscountAmount(safeLong(order.getCouponDiscountAmount()))
                .discountCodeText(order.getDiscountCodeText())
                .description(order.getDescription())
                .courseTitles(titles.stream().map(String::trim).filter(title -> !title.isBlank()).toList())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .build();
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private record PriceBreakdown(
            long originalAmount,
            long systemDiscountAmount,
            long subtotalAmount,
            long couponDiscountAmount,
            long totalAmount,
            DiscountCode discountCode,
            String couponCode,
            String couponMessage
    ) {
    }

    private record PayableBundle(List<OnlineCourse> onlineCourses) {
        List<LearningPackage> packages() {
            List<LearningPackage> result = new ArrayList<>();
            onlineCourses.forEach(course -> {
                if (course.getLearningPackage() != null) {
                    result.add(course.getLearningPackage());
                }
            });
            return result;
        }

        String allTitles() {
            return packages().stream()
                    .map(LearningPackage::getTitle)
                    .collect(Collectors.joining(" | "));
        }
    }
}
