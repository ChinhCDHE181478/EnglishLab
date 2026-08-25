package fu.sep490.g23.backend.service.payment.impl;


import fu.sep490.g23.backend.dto.request.payment.RefundCourseOrderRequest;
import fu.sep490.g23.backend.dto.response.payment.PaymentLinkResponse;
import fu.sep490.g23.backend.dto.response.payment.PaymentOrderStatusResponse;
import fu.sep490.g23.backend.dto.response.payment.PaymentOrderSummaryResponse;
import fu.sep490.g23.backend.dto.response.payment.PaymentQuoteResponse;
import fu.sep490.g23.backend.dto.response.payment.RevenueAnalyticsResponse;
import fu.sep490.g23.backend.dto.response.payment.RevenueByMonthResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomRegistrationStatus;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.LearningPath;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.payment.DiscountCode;
import fu.sep490.g23.backend.entity.payment.enums.DiscountType;
import fu.sep490.g23.backend.entity.payment.PaymentOrder;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.LearningPathCourseRepository;
import fu.sep490.g23.backend.repository.course.LearningPathRepository;
import fu.sep490.g23.backend.repository.payment.DiscountCodeRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sep490.g23.backend.service.classroom.ClassroomOfferingService;
import fu.sep490.g23.backend.service.classroom.ClassroomRegistrationSupport;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import fu.sep490.g23.backend.service.payment.PaymentReceiptPdfService;
import fu.sep490.g23.backend.service.payment.PaymentService;
import fu.sep490.g23.backend.service.payment.PayosProperties;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PayosProperties payosProperties;
    private final PaymentOrderRepository paymentOrderRepository;
    private final DiscountCodeRepository discountCodeRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final LearningPathRepository learningPathRepository;
    private final LearningPathCourseRepository learningPathCourseRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final UserRepository userRepository;
    private final OnlineCourseService onlineCourseService;
    private final ClassroomOfferingService classSectionService;
    private final PaymentReceiptPdfService paymentReceiptPdfService;

    @Override
    @Transactional(readOnly = true)
    public PaymentQuoteResponse quotePayment(List<Long> courseIds, List<Long> classSectionIds, String couponCode, String studentEmail) {
        return quotePayment(courseIds, classSectionIds, null, couponCode, studentEmail);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentQuoteResponse quotePayment(List<Long> courseIds, List<Long> classSectionIds, Long learningPathId, String couponCode, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người học."));
        PayableBundle bundle = resolvePayableBundle(courseIds, classSectionIds, learningPathId, student);
        return toQuoteResponse(calculateBreakdown(bundle, couponCode, false), bundle);
    }

    @Override
    public PaymentLinkResponse createPaymentLink(List<Long> courseIds, List<Long> classSectionIds, String couponCode, String studentEmail) {
        return createPaymentLink(courseIds, classSectionIds, null, couponCode, studentEmail);
    }

    @Override
    public PaymentLinkResponse createPaymentLink(List<Long> courseIds, List<Long> classSectionIds, Long learningPathId, String couponCode, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người học."));
        PayableBundle bundle = resolvePayableBundle(courseIds, classSectionIds, learningPathId, student);
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
                    .learningPathDiscountAmount(breakdown.learningPathDiscountAmount())
                    .couponDiscountAmount(breakdown.couponDiscountAmount())
                    .totalAmount(0L)
                    .couponCode(breakdown.couponCode())
                    .learningPathId(bundle.learningPathId())
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
                .classSectionIdsCsv(bundle.classroomTuitions().stream()
                        .map(item -> String.valueOf(item.offering().getId()))
                        .collect(Collectors.joining(",")))
                .enrollmentId(bundle.primaryEnrollmentId())
                .courseTitles(bundle.allTitles())
                .learningPathId(bundle.learningPathId())
                .learningPathCode(bundle.learningPathCode())
                .amount(amount)
                .originalAmount(breakdown.originalAmount())
                .systemDiscountAmount(breakdown.systemDiscountAmount())
                .learningPathDiscountAmount(breakdown.learningPathDiscountAmount())
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
            String itemName = bundle.isClassroomTuition() ? "EnglishLab classroom tuition" : "EnglishLab order";
            List<PaymentLinkItem> items = List.of(PaymentLinkItem.builder()
                    .name(itemName)
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
                    .learningPathDiscountAmount(paymentOrder.getLearningPathDiscountAmount())
                    .couponDiscountAmount(paymentOrder.getCouponDiscountAmount())
                    .totalAmount(paymentOrder.getAmount())
                    .couponCode(paymentOrder.getDiscountCodeText())
                    .learningPathId(paymentOrder.getLearningPathId())
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
                .message(resolveStatusMessage(order))
                .classSectionId(firstClassSectionId(order))
                .enrollmentId(order.getEnrollmentId())
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
        if (bundle.isClassroomTuition()) {
            if (normalizeCouponCode(couponCode) != null) {
                throw new RuntimeException("Mã giảm giá hiện chưa áp dụng cho học phí lớp học.");
            }
            long amount = bundle.classroomTuitions().stream().mapToLong(PayableClassroomTuition::amountVnd).sum();
            return new PriceBreakdown(
                    amount,
                    0L,
                    0L,
                    amount,
                    0L,
                    amount,
                    null,
                    null,
                    null
            );
        }

        List<LearningPackage> packages = bundle.packages();
        long originalAmount = packages.stream().mapToLong(pkg -> toVnd(resolveOriginalPrice(pkg))).sum();
        long courseSubtotalAmount = packages.stream().mapToLong(pkg -> toVnd(resolveSystemPrice(pkg))).sum();
        long systemDiscountAmount = Math.max(0L, originalAmount - courseSubtotalAmount);
        long learningPathDiscountAmount = calculateLearningPathDiscount(bundle, courseSubtotalAmount);
        long subtotalAmount = Math.max(0L, courseSubtotalAmount - learningPathDiscountAmount);
        DiscountCode discountCode = resolveDiscountCode(couponCode, reserveCoupon);
        long couponDiscountAmount = calculateCouponDiscount(discountCode, subtotalAmount);
        long totalAmount = Math.max(0L, subtotalAmount - couponDiscountAmount);
        return new PriceBreakdown(
                originalAmount,
                systemDiscountAmount,
                learningPathDiscountAmount,
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

    private PaymentQuoteResponse toQuoteResponse(PriceBreakdown breakdown, PayableBundle bundle) {
        return PaymentQuoteResponse.builder()
                .originalAmount(breakdown.originalAmount())
                .systemDiscountAmount(breakdown.systemDiscountAmount())
                .learningPathDiscountAmount(breakdown.learningPathDiscountAmount())
                .subtotalAmount(breakdown.subtotalAmount())
                .couponDiscountAmount(breakdown.couponDiscountAmount())
                .totalAmount(breakdown.totalAmount())
                .couponCode(breakdown.couponCode())
                .couponMessage(breakdown.couponMessage())
                .learningPathId(bundle.learningPathId())
                .learningPathName(bundle.learningPathName())
                .build();
    }

    private long calculateLearningPathDiscount(PayableBundle bundle, long subtotalAmount) {
        LearningPath path = bundle.learningPath();
        if (path == null || subtotalAmount <= 0) return 0L;
        int minimumCourses = path.getMinimumCoursesForDiscount() == null
                ? 2
                : Math.max(2, path.getMinimumCoursesForDiscount());
        int discountPercent = path.getDiscountPercent() == null
                ? 0
                : Math.min(100, Math.max(0, path.getDiscountPercent()));
        if (bundle.onlineCourses().size() < minimumCourses || discountPercent <= 0) return 0L;
        return BigDecimal.valueOf(subtotalAmount)
                .multiply(BigDecimal.valueOf(discountPercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValue();
    }

    private void markOrderPaid(PaymentOrder order) {
        if (order.getStatus() != PaymentOrderStatus.PAID) {
            order.setStatus(PaymentOrderStatus.PAID);
            order.setPaidAt(LocalDateTime.now());
            consumeCouponReservation(order);
            enrollPurchasedCourses(order);
            applyClassroomTuitionIfNeeded(order);
        }
    }

    private void applyClassroomTuitionIfNeeded(PaymentOrder order) {
        if (order.getEnrollmentId() == null) {
            return;
        }
        BigDecimal amount = BigDecimal.valueOf(safeLong(order.getAmount()));
        String note = "PayOS #" + order.getOrderCode();
        classSectionService.applyPayosTuitionPayment(order.getEnrollmentId(), amount, note);
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

    private PayableBundle resolvePayableBundle(
            List<Long> courseIds,
            List<Long> classSectionIds,
            Long learningPathId,
            User student
    ) {
        List<Long> normalizedCourseIds = normalizeIds(courseIds);
        List<Long> normalizedClassroomIds = normalizeIds(classSectionIds);
        if (normalizedClassroomIds.isEmpty() && normalizedCourseIds.isEmpty() && learningPathId == null) {
            throw new RuntimeException("Không có khóa học hoặc lớp học hợp lệ để thanh toán.");
        }
        if (!normalizedClassroomIds.isEmpty() && (!normalizedCourseIds.isEmpty() || learningPathId != null)) {
            throw new RuntimeException("Không thể thanh toán khóa học online và học phí lớp trong cùng một đơn.");
        }
        if (!normalizedClassroomIds.isEmpty()) {
            return new PayableBundle(List.of(), resolvePayableClassroomTuitions(normalizedClassroomIds, student), null);
        }
        if (learningPathId != null) {
            return resolvePayableLearningPath(learningPathId, student);
        }
        List<OnlineCourse> courses = resolvePayableCourses(normalizedCourseIds, student);
        return new PayableBundle(courses, List.of(), null);
    }

    private PayableBundle resolvePayableLearningPath(Long learningPathId, User student) {
        LearningPath path = learningPathRepository.findById(learningPathId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy lộ trình."));
        Set<Long> enrolledCourseIds = onlineCourseService.getMyEnrollments(student.getEmail()).stream()
                .map(item -> item.getCourseId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<OnlineCourse> remainingCourses = learningPathCourseRepository
                .findByLearningPathIdOrderByDisplayOrderAscIdAsc(path.getId()).stream()
                .map(ref -> ref.getOnlineCourse())
                .filter(course -> course.getLearningPackage() != null
                        && course.getLearningPackage().isPublished())
                .filter(course -> !enrolledCourseIds.contains(course.getId()))
                .toList();
        if (remainingCourses.isEmpty()) {
            throw new RuntimeException("Bạn đã sở hữu toàn bộ khóa học trong lộ trình này.");
        }
        return new PayableBundle(remainingCourses, List.of(), path);
    }

    private List<PayableClassroomTuition> resolvePayableClassroomTuitions(List<Long> classSectionIds, User student) {
        if (classSectionIds.size() != 1) {
            throw new RuntimeException("Mỗi lần chỉ thanh toán học phí cho một lớp học.");
        }

        Long offeringId = classSectionIds.getFirst();
        ClassEnrollment enrollment = classEnrollmentRepository
                .findByStudentIdAndClassSectionId(student.getId(), offeringId)
                .filter(item -> ClassroomRegistrationSupport.ACTIVE_REGISTRATIONS.contains(item.getRegistrationStatus()))
                .orElseThrow(() -> new RuntimeException("Bạn chưa có đăng ký hiệu lực cho lớp này."));

        ClassroomRegistrationStatus status = enrollment.getRegistrationStatus();
        if (status == ClassroomRegistrationStatus.WAITLIST) {
            throw new RuntimeException("Bạn đang ở trong danh sách chờ và chưa cần thanh toán học phí.");
        }
        if (status == ClassroomRegistrationStatus.ASSIGNED) {
            throw new RuntimeException("Bạn đã được xếp lớp. Không cần thanh toán thêm qua PayOS.");
        }
        if (status == ClassroomRegistrationStatus.CANCELLED || status == ClassroomRegistrationStatus.REJECTED) {
            throw new RuntimeException("Đăng ký đã bị hủy hoặc từ chối.");
        }

        BigDecimal balance = enrollment.tuitionBalance();
        if (balance.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Học phí lớp này đã được thanh toán đủ.");
        }

        if (paymentOrderRepository.existsByEnrollmentIdAndStatusIn(
                enrollment.getId(),
                List.of(PaymentOrderStatus.PENDING, PaymentOrderStatus.PROCESSING)
        )) {
            throw new RuntimeException("Bạn đang có đơn PayOS học phí chưa hoàn tất cho lớp này. Vui lòng hoàn tất hoặc chờ hết hạn trước khi tạo đơn mới.");
        }

        ClassSection offering = enrollment.getClassSection();
        String title = offering.getLearningPackage() == null
                ? "Lớp #" + offering.getId()
                : offering.getLearningPackage().getTitle();
        return List.of(new PayableClassroomTuition(enrollment, offering, toVnd(balance), title));
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

    private String resolveStatusMessage(PaymentOrder order) {
        PaymentOrderStatus status = order.getStatus();
        boolean classroomTuition = order.getEnrollmentId() != null;
        return switch (status) {
            case PAID -> classroomTuition
                    ? "Thanh toán thành công. Học phí lớp đã được ghi nhận vào hồ sơ đăng ký của bạn."
                    : "Thanh toán thành công. Khóa học đã được cập nhật vào tài khoản của bạn.";
            case REFUNDED -> "Đơn hàng đã được hoàn tiền trên hệ thống EnglishLab.";
            case PENDING, PROCESSING -> "Đơn thanh toán đang chờ PayOS xác nhận. Vui lòng tải lại sau vài giây.";
            case CANCELLED -> "Đơn thanh toán đã bị hủy.";
            case EXPIRED -> "Link thanh toán đã hết hạn.";
            case FAILED -> "Thanh toán chưa thành công. Vui lòng thử lại.";
        };
    }

    private Long firstClassSectionId(PaymentOrder order) {
        if (order.getClassSectionIdsCsv() == null || order.getClassSectionIdsCsv().isBlank()) {
            return null;
        }
        return parseCourseIds(order.getClassSectionIdsCsv()).stream().findFirst().orElse(null);
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
      public Page<PaymentOrderSummaryResponse> pageMyOrders(String studentEmail, Pageable pageable) {
          User student = userRepository.findByEmail(studentEmail)
                  .orElseThrow(() -> new RuntimeException("Không tìm thấy người học."));
          return paymentOrderRepository.findByStudent(student, pageable).map(this::toOrderSummary);
      }

    @Override
    @Transactional(readOnly = true)
    public Page<PaymentOrderSummaryResponse> listStaffOrders(PaymentOrderStatus status, Pageable pageable) {
        Page<PaymentOrder> orders = status == null
                ? paymentOrderRepository.findAll(pageable)
                : paymentOrderRepository.findByStatus(status, pageable);
        return orders.map(this::toOrderSummary);
    }

    @Override
    public PaymentOrderSummaryResponse refundCourseOrder(
            Long orderCode,
            RefundCourseOrderRequest request,
            String actorEmail
    ) {
        User actor = userRepository.findByEmail(actorEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản thao tác."));
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn thanh toán."));
        assertCourseOrder(order);
        if (order.getStatus() == PaymentOrderStatus.REFUNDED) {
            throw new RuntimeException("Đơn hàng này đã được hoàn tiền.");
        }
        if (order.getStatus() != PaymentOrderStatus.PAID) {
            throw new RuntimeException("Chỉ hoàn tiền được các đơn đã thanh toán thành công.");
        }

        String reason = request == null || request.getReason() == null ? "" : request.getReason().trim();
        if (reason.isBlank()) {
            throw new RuntimeException("Vui lòng nhập lý do hoàn tiền.");
        }

        order.setStatus(PaymentOrderStatus.REFUNDED);
        order.setRefundedAmount(safeLong(order.getAmount()));
        order.setRefundedAt(LocalDateTime.now());
        order.setRefundReason(reason);
        order.setRefundedBy(actor);
        paymentOrderRepository.save(order);

        String studentEmail = order.getStudent().getEmail();
        for (Long courseId : parseCourseIds(order.getCourseIdsCsv())) {
            onlineCourseService.revokePaidCourseAccess(courseId, studentEmail);
        }
        restoreCouponUsage(order);
        return toOrderSummary(order);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] downloadCourseReceipt(Long orderCode, String studentEmail) {
        PaymentOrder order = paymentOrderRepository.findByOrderCode(orderCode)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đơn thanh toán."));
        if (!Objects.equals(order.getStudent().getEmail(), studentEmail)) {
            throw new RuntimeException("Bạn không có quyền tải biên lai đơn này.");
        }
        assertCourseOrder(order);
        if (order.getStatus() != PaymentOrderStatus.PAID && order.getStatus() != PaymentOrderStatus.REFUNDED) {
            throw new RuntimeException("Chỉ tải biên lai được khi đơn đã thanh toán hoặc đã hoàn tiền.");
        }
        return paymentReceiptPdfService.buildCourseReceipt(order);
    }

    private void assertCourseOrder(PaymentOrder order) {
        if (order.getEnrollmentId() != null) {
            throw new RuntimeException("Đơn học phí lớp không hỗ trợ hoàn tiền/biên lai khóa học trong luồng này.");
        }
        if (order.getCourseIdsCsv() == null || order.getCourseIdsCsv().isBlank()) {
            throw new RuntimeException("Đơn hàng không chứa khóa học online hợp lệ.");
        }
    }

    private void restoreCouponUsage(PaymentOrder order) {
        if (order.getDiscountCode() == null || !order.isCouponReservationReleased()) {
            return;
        }
        DiscountCode discountCode = order.getDiscountCode();
        discountCode.setUsedCount(Math.max(0, safeCount(discountCode.getUsedCount()) - 1));
    }

    @Override
    @Transactional(readOnly = true)
    public RevenueAnalyticsResponse getRevenueAnalytics() {
        long totalOrders = paymentOrderRepository.count();
        long paid = paymentOrderRepository.countByStatus(PaymentOrderStatus.PAID);
        long failed = paymentOrderRepository.countByStatusIn(List.of(
                PaymentOrderStatus.FAILED,
                PaymentOrderStatus.CANCELLED,
                PaymentOrderStatus.EXPIRED,
                PaymentOrderStatus.REFUNDED
        ));
        long pending = paymentOrderRepository.countByStatusIn(List.of(
                PaymentOrderStatus.PENDING,
                PaymentOrderStatus.PROCESSING
        ));
        long totalRevenue = safeLong(paymentOrderRepository.sumAmountByStatus(PaymentOrderStatus.PAID));
        long totalDiscount = safeLong(paymentOrderRepository.sumDiscountByStatus(PaymentOrderStatus.PAID));
        long couponDiscount = safeLong(paymentOrderRepository.sumCouponDiscountByStatus(PaymentOrderStatus.PAID));
        List<RevenueByMonthResponse> monthly = paymentOrderRepository
                .summarizeMonthlyRevenue(PaymentOrderStatus.PAID)
                .stream()
                .map(entry -> RevenueByMonthResponse.builder()
                        .month(String.format("%04d-%02d", entry.getYearValue(), entry.getMonthValue()))
                        .revenueVnd(safeLong(entry.getRevenueVnd()))
                        .orderCount(safeLong(entry.getOrderCount()))
                        .build())
                .toList();

        return RevenueAnalyticsResponse.builder()
                .totalOrders(totalOrders)
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
        boolean courseOrder = order.getEnrollmentId() == null
                && order.getCourseIdsCsv() != null
                && !order.getCourseIdsCsv().isBlank();
        boolean classroomTuition = order.getEnrollmentId() != null;
        User student = order.getStudent();
        return PaymentOrderSummaryResponse.builder()
                .orderCode(order.getOrderCode())
                .status(order.getStatus().name())
                .paid(order.getStatus() == PaymentOrderStatus.PAID)
                .amount(safeLong(order.getAmount()))
                .originalAmount(safeLong(order.getOriginalAmount()))
                .systemDiscountAmount(safeLong(order.getSystemDiscountAmount()))
                .learningPathDiscountAmount(safeLong(order.getLearningPathDiscountAmount()))
                .couponDiscountAmount(safeLong(order.getCouponDiscountAmount()))
                .discountCodeText(order.getDiscountCodeText())
                .description(order.getDescription())
                .courseTitles(titles.stream().map(String::trim).filter(title -> !title.isBlank()).toList())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .orderType(classroomTuition ? "CLASSROOM_TUITION" : "COURSE")
                .refundable(courseOrder && order.getStatus() == PaymentOrderStatus.PAID)
                .hasReceipt(courseOrder && (order.getStatus() == PaymentOrderStatus.PAID
                        || order.getStatus() == PaymentOrderStatus.REFUNDED))
                .refundedAmount(safeLong(order.getRefundedAmount()))
                .refundedAt(order.getRefundedAt())
                .refundReason(order.getRefundReason())
                .studentEmail(student == null ? null : student.getEmail())
                .studentName(student == null ? null : student.getFullName())
                .learningPathId(order.getLearningPathId())
                .learningPathCode(order.getLearningPathCode())
                .build();
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private record PriceBreakdown(
            long originalAmount,
            long systemDiscountAmount,
            long learningPathDiscountAmount,
            long subtotalAmount,
            long couponDiscountAmount,
            long totalAmount,
            DiscountCode discountCode,
            String couponCode,
            String couponMessage
    ) {
    }

    private record PayableClassroomTuition(
            ClassEnrollment enrollment,
            ClassSection offering,
            long amountVnd,
            String title
    ) {
    }

    private record PayableBundle(
            List<OnlineCourse> onlineCourses,
            List<PayableClassroomTuition> classroomTuitions,
            LearningPath learningPath
    ) {
        boolean isClassroomTuition() {
            return classroomTuitions != null && !classroomTuitions.isEmpty();
        }

        Long primaryEnrollmentId() {
            return isClassroomTuition() ? classroomTuitions.getFirst().enrollment().getId() : null;
        }

        Long learningPathId() {
            return learningPath == null ? null : learningPath.getId();
        }

        String learningPathCode() {
            return learningPath == null ? null : learningPath.getCode();
        }

        String learningPathName() {
            return learningPath == null ? null : learningPath.getName();
        }

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
            if (isClassroomTuition()) {
                return classroomTuitions.stream()
                        .map(PayableClassroomTuition::title)
                        .collect(Collectors.joining(" | "));
            }
            return packages().stream()
                    .map(LearningPackage::getTitle)
                    .collect(Collectors.joining(" | "));
        }
    }
}
