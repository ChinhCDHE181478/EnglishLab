package fu.sep490.g23.backend.seed;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.payment.PaymentOrder;
import fu.sep490.g23.backend.entity.enums.RoleEnum;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.course.LearningPackageRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sep490.g23.backend.repository.payment.PaymentOrderRepository;
import fu.sep490.g23.backend.service.user.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(200)
@RequiredArgsConstructor
public class LearnerAccountRoleRepairSeeder implements CommandLineRunner {

    static final String LEARNER_EMAIL = "0386852628z@gmail.com";
    private static final String DEMO_REPURCHASE_COURSE_SLUG = "e2-ielts-practice-tests";

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final DemoLearnerOnboardingSupport demoLearnerOnboardingSupport;
    private final LearningPackageRepository learningPackageRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final PackageEnrollmentRepository packageEnrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final PaymentOrderRepository paymentOrderRepository;

    @Override
    @Transactional
    public void run(String... args) {
        userRepository.findByEmail(LEARNER_EMAIL).ifPresent(user -> {
            ensureLearnerRoleOnly(user);
            demoLearnerOnboardingSupport.ensureReady(user);
            resetDemoRepurchaseCourse(user);
        });
    }

    private void ensureLearnerRoleOnly(User user) {
        if (user.getRoles().size() == 1 && user.hasRole(RoleEnum.LEARNER)) {
            return;
        }
        userRoleService.replaceRoles(user, RoleEnum.LEARNER);
        userRepository.save(user);
    }

    private void resetDemoRepurchaseCourse(User user) {
        learningPackageRepository.findBySlugAndDeletedFalse(DEMO_REPURCHASE_COURSE_SLUG)
                .ifPresent(learningPackage -> {
                    removeEnrollmentAccess(user, learningPackage);
                    removePaymentHistoryForCourse(user, learningPackage);
                });
    }

    private void removeEnrollmentAccess(User user, LearningPackage learningPackage) {
        packageEnrollmentRepository.findByStudentAndLearningPackage(user, learningPackage)
                .ifPresent(enrollment -> {
                    lessonProgressRepository.deleteAll(lessonProgressRepository.findByEnrollment(enrollment));
                    packageEnrollmentRepository.delete(enrollment);
                });
    }

    private void removePaymentHistoryForCourse(User user, LearningPackage learningPackage) {
        onlineCourseRepository.findByLearningPackage(learningPackage)
                .map(OnlineCourse::getId)
                .ifPresent(courseId -> paymentOrderRepository.findByStudentOrderByCreatedAtDesc(user).stream()
                        .filter(order -> containsCourseId(order, courseId))
                        .forEach(paymentOrderRepository::delete));
    }

    private boolean containsCourseId(PaymentOrder order, Long courseId) {
        if (order.getCourseIdsCsv() == null || order.getCourseIdsCsv().isBlank()) {
            return false;
        }
        Set<Long> courseIds = Arrays.stream(order.getCourseIdsCsv().split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(this::parseLongOrNull)
                .filter(value -> value != null)
                .collect(Collectors.toSet());
        return courseIds.contains(courseId);
    }

    private Long parseLongOrNull(String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
