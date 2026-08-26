package fu.sep490.g23.backend.seed;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassSection;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomEnrollmentStatus;
import fu.sep490.g23.backend.entity.course.LearningPath;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.payment.PaymentOrder;
import fu.sep490.g23.backend.entity.payment.PaymentOrderItem;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderItemType;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.LearningPathRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Order(335)
@RequiredArgsConstructor
@Slf4j
public class ShowcaseLearnerPaymentHistorySeeder implements CommandLineRunner {
    private static final String LEARNER_EMAIL = "0386852628z@gmail.com";
    private static final String PATH_CODE = "IELTS_BAND_55_TO_70";
    private static final Set<String> PATH_COURSE_SLUGS = Set.of(
            "ielts-master-vocabulary-band-7-plus",
            "e2-ielts-practice-tests"
    );
    private static final Set<ClassroomEnrollmentStatus> CLASSROOM_STATUSES = Set.of(
            ClassroomEnrollmentStatus.ENROLLED,
            ClassroomEnrollmentStatus.COMPLETED
    );

    private final UserRepository userRepository;
    private final OnlineCourseEnrollmentRepository packageEnrollmentRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final ClassEnrollmentRepository classEnrollmentRepository;
    private final PaymentOrderRepository paymentOrderRepository;
    private final PaymentOrderItemRepository paymentOrderItemRepository;
    private final LearningPathRepository learningPathRepository;

    @Value("${app.seed.sheet.enabled:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }
        User learner = userRepository.findByEmail(LEARNER_EMAIL).orElse(null);
        if (learner == null) {
            return;
        }
        syncShowcaseCoursePrices();

        List<PaymentOrder> existing = paymentOrderRepository.findByStudentOrderByCreatedAtDesc(learner);
        Set<Long> coveredCourseIds = coveredCourseIds(existing);
        Set<Long> coveredEnrollmentIds = coveredEnrollmentIds(existing);
        long orderCode = System.currentTimeMillis();
        int created = 0;

        List<OnlineCourseEnrollment> onlineEnrollments = packageEnrollmentRepository.findByStudentOrderByRegisteredAtDesc(learner);
        List<CoursePurchase> pathCourses = new ArrayList<>();
        List<CoursePurchase> otherCourses = new ArrayList<>();
        for (OnlineCourseEnrollment enrollment : onlineEnrollments) {
            CoursePurchase purchase = toPurchase(enrollment);
            if (purchase == null || coveredCourseIds.contains(purchase.courseId())) {
                continue;
            }
            if (PATH_COURSE_SLUGS.contains(purchase.slug())) {
                pathCourses.add(purchase);
            } else {
                otherCourses.add(purchase);
            }
        }

        LearningPath path = learningPathRepository.findByCodeIgnoreCase(PATH_CODE).orElse(null);
        if (pathCourses.size() >= 2) {
            orderCode = nextOrderCode(orderCode);
            saveOnlineOrder(bundlePathOrder(learner, pathCourses, path, orderCode), pathCourses);
            pathCourses.forEach(item -> coveredCourseIds.add(item.courseId()));
            created++;
        } else {
            otherCourses.addAll(pathCourses);
        }

        for (CoursePurchase purchase : otherCourses) {
            orderCode = nextOrderCode(orderCode);
            saveOnlineOrder(singleCourseOrder(learner, purchase, orderCode), List.of(purchase));
            created++;
        }

        List<ClassEnrollment> classEnrollments = classEnrollmentRepository
                .findByStudentIdAndStatusIn(learner.getId(), CLASSROOM_STATUSES);
        for (ClassEnrollment enrollment : classEnrollments) {
            if (coveredEnrollmentIds.contains(enrollment.getId())) {
                continue;
            }
            orderCode = nextOrderCode(orderCode);
            saveClassroomOrder(classroomOrder(learner, enrollment, orderCode), enrollment);
            created++;
        }

        if (created > 0) {
            log.info("Đã tạo {} đơn thanh toán demo cho {}.", created, LEARNER_EMAIL);
        }
    }

    private void syncShowcaseCoursePrices() {
        setPackagePrice("ielts-master-vocabulary-band-7-plus", 1_290_000);
        setPackagePrice("e2-ielts-practice-tests", 1_190_000);
    }

    private void setPackagePrice(String slug, long priceVnd) {
        onlineCourseRepository.findBySlug(slug).ifPresent(course -> {
            course.setPrice(BigDecimal.valueOf(priceVnd));
            onlineCourseRepository.save(course);
        });
    }

    private CoursePurchase toPurchase(OnlineCourseEnrollment enrollment) {
        OnlineCourse course = enrollment.getOnlineCourse();
        if (course == null) {
            return null;
        }
        LocalDateTime paidAt = enrollment.getRegisteredAt() == null
                ? LocalDateTime.now().minusDays(20)
                : enrollment.getRegisteredAt().plusHours(2);
        return new CoursePurchase(course.getId(), course.getSlug(), course.getTitle(), toVnd(course.getPrice()), paidAt);
    }

    private PaymentOrder bundlePathOrder(User learner, List<CoursePurchase> courses, LearningPath path, long orderCode) {
        long original = courses.stream().mapToLong(CoursePurchase::amount).sum();
        int discountPercent = path == null || path.getDiscountPercent() == null ? 15 : path.getDiscountPercent();
        long pathDiscount = BigDecimal.valueOf(original)
                .multiply(BigDecimal.valueOf(Math.max(0, discountPercent)))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP)
                .longValue();
        LocalDateTime paidAt = courses.stream()
                .map(CoursePurchase::paidAt)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now().minusDays(30));
        return baseOrder(learner, orderCode, paidAt)
                .learningPathId(path == null ? null : path.getId())
                .learningPathCode(path == null ? PATH_CODE : path.getCode())
                .originalAmount(original)
                .learningPathDiscountAmount(pathDiscount)
                .amount(Math.max(0L, original - pathDiscount))
                .build();
    }

    private PaymentOrder singleCourseOrder(User learner, CoursePurchase purchase, long orderCode) {
        return baseOrder(learner, orderCode, purchase.paidAt())
                .originalAmount(purchase.amount())
                .amount(purchase.amount())
                .build();
    }

    private PaymentOrder classroomOrder(User learner, ClassEnrollment enrollment, long orderCode) {
        ClassSection offering = enrollment.getClassSection();
        String title = offering != null && offering.getInstructorLedCourse() != null
                ? offering.getTitle()
                : "Học phí lớp";
        long amount = toVnd(enrollment.getTuitionAmountPaid() == null
                ? enrollment.getTuitionAmountDue()
                : enrollment.getTuitionAmountPaid());
        LocalDateTime paidAt = enrollment.getEnrolledAt() == null
                ? LocalDateTime.now().minusDays(12)
                : enrollment.getEnrolledAt().plusHours(3);
        return baseOrder(learner, orderCode, paidAt)
                .originalAmount(amount)
                .amount(amount)
                .build();
    }

    private PaymentOrder.PaymentOrderBuilder baseOrder(User learner, long orderCode, LocalDateTime paidAt) {
        String codeText = String.valueOf(orderCode);
        return PaymentOrder.builder()
                .orderCode(orderCode)
                .student(learner)
                .systemDiscountAmount(0L)
                .couponDiscountAmount(0L)
                .couponReservationReleased(true)
                .description("ELAB" + codeText.substring(Math.max(0, codeText.length() - 6)))
                .status(PaymentOrderStatus.PAID)
                .providerReference("SHOWCASE-SEED")
                .paidAt(paidAt)
                .webhookConfirmedAt(paidAt);
    }

    private void saveOnlineOrder(PaymentOrder order, List<CoursePurchase> purchases) {
        PaymentOrder saved = paymentOrderRepository.save(order);
        long allocated = 0L;
        List<PaymentOrderItem> items = new ArrayList<>();
        for (int index = 0; index < purchases.size(); index++) {
            CoursePurchase purchase = purchases.get(index);
            long finalAmount = index == purchases.size() - 1
                    ? order.getAmount() - allocated
                    : Math.floorDiv(order.getAmount() * purchase.amount(), Math.max(1L, order.getOriginalAmount()));
            allocated += finalAmount;
            OnlineCourse course = onlineCourseRepository.findById(purchase.courseId()).orElseThrow();
            items.add(PaymentOrderItem.builder()
                    .paymentOrder(saved)
                    .itemType(PaymentOrderItemType.ONLINE_COURSE)
                    .onlineCourse(course)
                    .titleSnapshot(purchase.title())
                    .unitPriceVnd(purchase.amount())
                    .discountAmountVnd(Math.max(0L, purchase.amount() - finalAmount))
                    .finalAmountVnd(finalAmount)
                    .quantity(1)
                    .build());
        }
        paymentOrderItemRepository.saveAll(items);
    }

    private void saveClassroomOrder(PaymentOrder order, ClassEnrollment enrollment) {
        PaymentOrder saved = paymentOrderRepository.save(order);
        ClassSection section = enrollment.getClassSection();
        String title = section != null && section.getInstructorLedCourse() != null
                ? section.getTitle()
                : "Học phí lớp";
        paymentOrderItemRepository.save(PaymentOrderItem.builder()
                .paymentOrder(saved)
                .itemType(PaymentOrderItemType.CLASS_ENROLLMENT)
                .classEnrollment(enrollment)
                .titleSnapshot(title)
                .unitPriceVnd(order.getOriginalAmount())
                .discountAmountVnd(Math.max(0L, order.getOriginalAmount() - order.getAmount()))
                .finalAmountVnd(order.getAmount())
                .quantity(1)
                .build());
    }

    private long nextOrderCode(long current) {
        long orderCode = current + 1;
        while (paymentOrderRepository.findByOrderCode(orderCode).isPresent()) {
            orderCode += 1;
        }
        return orderCode;
    }

    private Set<Long> coveredCourseIds(List<PaymentOrder> orders) {
        return orders.stream()
                .flatMap(order -> paymentOrderItemRepository.findByPaymentOrderIdOrderById(order.getId()).stream())
                .filter(item -> item.getItemType() == PaymentOrderItemType.ONLINE_COURSE)
                .map(PaymentOrderItem::getOnlineCourse)
                .filter(java.util.Objects::nonNull)
                .map(OnlineCourse::getId)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }

    private Set<Long> coveredEnrollmentIds(List<PaymentOrder> orders) {
        return orders.stream()
                .flatMap(order -> paymentOrderItemRepository.findByPaymentOrderIdOrderById(order.getId()).stream())
                .filter(item -> item.getItemType() == PaymentOrderItemType.CLASS_ENROLLMENT)
                .map(PaymentOrderItem::getClassEnrollment)
                .filter(java.util.Objects::nonNull)
                .map(ClassEnrollment::getId)
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
    }

    private long toVnd(BigDecimal value) {
        if (value == null || value.signum() <= 0) {
            return 0L;
        }
        return value.setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private record CoursePurchase(Long courseId, String slug, String title, long amount, LocalDateTime paidAt) {
    }
}
