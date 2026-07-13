package fu.sap490.g23.backend.seed;

import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.course.Lesson;
import fu.sap490.g23.backend.entity.course.LessonProgress;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sap490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sap490.g23.backend.entity.enums.RoleEnum;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.LessonProgressRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sap490.g23.backend.service.user.UserRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/** Demo data used by the public catalog and certificate verification walkthrough. */
@Component
@Order(250)
@RequiredArgsConstructor
public class CertificateAndCatalogDemoSeeder implements CommandLineRunner {

    public static final String CERTIFICATE_LEARNER_EMAIL = "certificate.learner@englishlab.vn";
    private static final String CATALOG_DEMO_SLUG = "abc";
    private static final String PRIMARY_PATH_CODE = "IELTS_BAND_55_TO_70";
    private static final String PRIMARY_PATH_NAME = "IELTS 5.5 to 7.0 Self-Paced Path";

    private final UserRepository userRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final LearningPackageRepository learningPackageRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final PackageEnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) return;

        normalizePrimaryPathOrder("ielts-master-vocabulary-band-7-plus", 1);
        normalizePrimaryPathOrder("e2-ielts-practice-tests", 2);
        learningPackageRepository.findBySlugAndDeletedFalse(CATALOG_DEMO_SLUG)
                .flatMap(onlineCourseRepository::findByLearningPackage)
                .ifPresent(this::prepareCatalogAndCertificateDemo);
    }

    private void normalizePrimaryPathOrder(String slug, int order) {
        learningPackageRepository.findBySlugAndDeletedFalse(slug)
                .flatMap(onlineCourseRepository::findByLearningPackage)
                .ifPresent(course -> {
                    course.setLearningPathCode(PRIMARY_PATH_CODE);
                    course.setLearningPathName(PRIMARY_PATH_NAME);
                    course.setLearningPathOrder(order);
                    onlineCourseRepository.save(course);
                });
    }

    private void prepareCatalogAndCertificateDemo(OnlineCourse course) {
        // Keep a second visible path in the public course catalog without changing the two IELTS seed courses.
        course.setLearningPathCode("IELTS_FOUNDATION_REVIEW");
        course.setLearningPathName("Lộ trình IELTS Foundation");
        course.setLearningPathOrder(1);
        course.setRecommendedNextCourseSlug(null);
        onlineCourseRepository.save(course);

        User learner = userRepository.findByEmail(CERTIFICATE_LEARNER_EMAIL).orElseGet(() -> {
            User created = User.builder()
                    .email(CERTIFICATE_LEARNER_EMAIL)
                    .fullName("Học viên Chứng nhận Demo")
                    .password(passwordEncoder.encode("Password123!"))
                    .emailVerified(true)
                    .profileCompleted(true)
                    .currentBand(6.0)
                    .targetExam("IELTS")
                    .targetScore("7.0")
                    .studyGoal("Hoàn thành khóa học demo để kiểm tra chứng nhận.")
                    .build();
            userRoleService.assignRole(created, RoleEnum.LEARNER);
            return userRepository.save(created);
        });

        PackageEnrollment enrollment = enrollmentRepository.findByStudentAndLearningPackage(learner, course.getLearningPackage())
                .orElseGet(() -> enrollmentRepository.save(PackageEnrollment.builder()
                        .student(learner)
                        .learningPackage(course.getLearningPackage())
                        .registeredAt(LocalDateTime.now().minusDays(14))
                        .build()));
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollment.setProgressPercent(100);
        enrollmentRepository.save(enrollment);

        course.getModules().forEach(module -> module.getLessons().forEach(lesson -> completeLesson(learner, enrollment, lesson)));
    }

    private void completeLesson(User learner, PackageEnrollment enrollment, Lesson lesson) {
        LessonProgress progress = lessonProgressRepository.findByStudentAndLesson(learner, lesson)
                .orElseGet(() -> LessonProgress.builder().student(learner).lesson(lesson).enrollment(enrollment).build());
        progress.setEnrollment(enrollment);
        progress.setStatus(LessonProgressStatus.COMPLETED);
        progress.setProgressPercent(100);
        progress.setCompletedAt(LocalDateTime.now().minusDays(7));
        progress.setLastAccessedAt(LocalDateTime.now().minusDays(7));
        lessonProgressRepository.save(progress);
    }
}
