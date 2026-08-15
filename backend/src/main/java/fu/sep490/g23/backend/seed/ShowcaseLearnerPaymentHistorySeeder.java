package fu.sep490.g23.backend.seed;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.classroom.ClassroomEnrollment;
import fu.sep490.g23.backend.entity.classroom.ClassroomOffering;
import fu.sep490.g23.backend.entity.classroom.enums.ClassroomEnrollmentStatus;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.LearningPath;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.PackageEnrollment;
import fu.sep490.g23.backend.entity.payment.PaymentOrder;
import fu.sep490.g23.backend.entity.payment.enums.PaymentOrderStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.classroom.ClassroomEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.LearningPackageRepository;
import fu.sep490.g23.backend.repository.course.LearningPathRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderRepository;
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
import java.util.Arrays;
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
    private final PackageEnrollmentRepository packageEnrollmentRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final ClassroomEnrollmentRepository classroomEnrollmentRepository;
    private final PaymentOrderRepository paymentOrderRepository;
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

        List<PackageEnrollment> onlineEnrollments = packageEnrollmentRepository.findByStudentOrderByRegisteredAtDesc(learner);
        List<CoursePurchase> pathCourses = new ArrayList<>();
        List<CoursePurchase> otherCourses = new ArrayList<>();
        for (PackageEnrollment enrollment : onlineEnrollments) {
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
            saveOrder(bundlePathOrder(learner, pathCourses, path, orderCode));
            pathCourses.forEach(item -> coveredCourseIds.add(item.courseId()));
            created++;
        } else {
            otherCourses.addAll(pathCourses);
        }

        for (CoursePurchase purchase : otherCourses) {
            orderCode = nextOrderCode(orderCode);
            saveOrder(singleCourseOrder(learner, purchase, orderCode));
            created++;
        }

        List<ClassroomEnrollment> classroomEnrollments = classroomEnrollmentRepository
                .findByStudentIdAndStatusIn(learner.getId(), CLASSROOM_STATUSES);
        for (ClassroomEnrollment enrollment : classroomEnrollments) {
            if (coveredEnrollmentIds.contains(enrollment.getId())) {
                continue;
            }
            orderCode = nextOrderCode(orderCode);
            saveOrder(classroomOrder(learner, enrollment, orderCode));
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
        learningPackageRepository.findBySlugAndDeletedFalse(slug).ifPresent(pack -> {
            pack.setPrice(BigDecimal.valueOf(priceVnd));
            learningPackageRepository.save(pack);
        });
    }

    private CoursePurchase toPurchase(PackageEnrollment enrollment) {
        LearningPackage pack = enrollment.getLearningPackage();
        if (pack == null) {
            return null;
        }
        OnlineCourse course = onlineCourseRepository.findByLearningPackage(pack).orElse(null);
        if (course == null) {
            return null;
        }
        LocalDateTime paidAt = enrollment.getRegisteredAt() == null
                ? LocalDateTime.now().minusDays(20)
                : enrollment.getRegisteredAt().plusHours(2);
        return new CoursePurchase(course.getId(), pack.getSlug(), pack.getTitle(), toVnd(pack.getPrice()), paidAt);
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
                .courseIdsCsv(joinIds(courses.stream().map(CoursePurchase::courseId).toList()))
                .courseTitles(String.join("|", courses.stream().map(CoursePurchase::title).toList()))
                .learningPathId(path == null ? null : path.getId())
                .learningPathCode(path == null ? PATH_CODE : path.getCode())
                .originalAmount(original)
                .learningPathDiscountAmount(pathDiscount)
                .amount(Math.max(0L, original - pathDiscount))
                .build();
    }

    private PaymentOrder singleCourseOrder(User learner, CoursePurchase purchase, long orderCode) {
        return baseOrder(learner, orderCode, purchase.paidAt())
                .courseIdsCsv(String.valueOf(purchase.courseId()))
                .courseTitles(purchase.title())
                .originalAmount(purchase.amount())
                .amount(purchase.amount())
                .build();
    }

    private PaymentOrder classroomOrder(User learner, ClassroomEnrollment enrollment, long orderCode) {
        ClassroomOffering offering = enrollment.getClassroomOffering();
        String title = offering != null && offering.getLearningPackage() != null
                ? offering.getLearningPackage().getTitle()
                : "Học phí lớp";
        long amount = toVnd(enrollment.getTuitionAmountPaid() == null
                ? enrollment.getTuitionAmountDue()
                : enrollment.getTuitionAmountPaid());
        LocalDateTime paidAt = enrollment.getEnrolledAt() == null
                ? LocalDateTime.now().minusDays(12)
                : enrollment.getEnrolledAt().plusHours(3);
        return baseOrder(learner, orderCode, paidAt)
                .courseIdsCsv("")
                .classroomOfferingIdsCsv(offering == null ? "" : String.valueOf(offering.getId()))
                .enrollmentId(enrollment.getId())
                .courseTitles(title)
                .originalAmount(amount)
                .amount(amount)
                .build();
    }

    private PaymentOrder.PaymentOrderBuilder baseOrder(User learner, long orderCode, LocalDateTime paidAt) {
        String codeText = String.valueOf(orderCode);
        return PaymentOrder.builder()
                .orderCode(orderCode)
                .student(learner)
                .classroomOfferingIdsCsv("")
                .systemDiscountAmount(0L)
                .couponDiscountAmount(0L)
                .couponReservationReleased(true)
                .description("ELAB" + codeText.substring(Math.max(0, codeText.length() - 6)))
                .status(PaymentOrderStatus.PAID)
                .providerReference("SHOWCASE-SEED")
                .paidAt(paidAt)
                .webhookConfirmedAt(paidAt);
    }

    private void saveOrder(PaymentOrder order) {
        paymentOrderRepository.save(order);
    }

    private long nextOrderCode(long current) {
        long orderCode = current + 1;
        while (paymentOrderRepository.findByOrderCode(orderCode).isPresent()) {
            orderCode += 1;
        }
        return orderCode;
    }

    private Set<Long> coveredCourseIds(List<PaymentOrder> orders) {
        Set<Long> ids = new HashSet<>();
        for (PaymentOrder order : orders) {
            if (order.getCourseIdsCsv() == null || order.getCourseIdsCsv().isBlank()) {
                continue;
            }
            Arrays.stream(order.getCourseIdsCsv().split(","))
                    .map(String::trim)
                    .filter(value -> !value.isBlank())
                    .forEach(value -> {
                        try {
                            ids.add(Long.parseLong(value));
                        } catch (NumberFormatException ignored) {
                            // ignore malformed seed/history rows
                        }
                    });
        }
        return ids;
    }

    private Set<Long> coveredEnrollmentIds(List<PaymentOrder> orders) {
        Set<Long> ids = new HashSet<>();
        for (PaymentOrder order : orders) {
            if (order.getEnrollmentId() != null) {
                ids.add(order.getEnrollmentId());
            }
        }
        return ids;
    }

    private String joinIds(List<Long> ids) {
        return String.join(",", ids.stream().map(String::valueOf).toList());
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
