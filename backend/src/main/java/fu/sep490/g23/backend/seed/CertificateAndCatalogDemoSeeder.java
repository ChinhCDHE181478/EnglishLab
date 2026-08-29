package fu.sep490.g23.backend.seed;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.admin.AuditLog;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.LessonProgress;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.enums.CourseCategoryCode;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.enums.RoleCodes;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.admin.AuditLogRepository;
import fu.sep490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import fu.sep490.g23.backend.service.user.UserRoleService;
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
    private static final String LONG_CONTENT_MANAGER_EMAIL = "longnthe182112@fpt.edu.vn";
    private static final String CATALOG_DEMO_SLUG = "abc";
    private static final String PRIMARY_PATH_CODE = "IELTS_BAND_55_TO_70";
    private static final String PRIMARY_PATH_NAME = "IELTS 5.5 to 7.0 Self-Paced Path";

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final UserRoleService userRoleService;
    private final PasswordEncoder passwordEncoder;
    private final CourseCategoryRepository courseCategoryRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseEnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final OnlineCourseVersionRepository onlineCourseVersionRepository;
    private final OnlineCourseVersionService onlineCourseVersionService;

    @Value("${app.seed.test.enabled:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) return;

        normalizePrimaryPathOrder("ielts-master-vocabulary-band-7-plus", 1);
        normalizePrimaryPathOrder("e2-ielts-practice-tests", 2);
        seedFoundationCatalogCourses();
        assignCatalogToLongContentManager();
        seedAuditLogs();
        onlineCourseRepository.findBySlug(CATALOG_DEMO_SLUG)
                .ifPresent(this::prepareCatalogAndCertificateDemo);
    }

    private void seedAuditLogs() {
        if (auditLogRepository.count() > 0) return;
        auditLogRepository.saveAll(java.util.List.of(
                audit("classroom.admin@englishlab.vn", "ADMIN_USER_CREATED", "USER", "22", "Tạo tài khoản học viên chứng nhận demo."),
                audit("classroom.admin@englishlab.vn", "ADMIN_USER_ROLES_UPDATED", "USER", "6", "Cập nhật vai trò giáo viên demo."),
                audit("content.manager@englishlab.vn", "DISCUSSION_CONTENT_HIDDEN", "THREAD", "1", "Ẩn nội dung hỏi đáp được báo cáo."),
                audit("classroom.admin@englishlab.vn", "ADMIN_USER_STATUS_UPDATED", "USER", "24", "Xác nhận trạng thái tài khoản quản trị." )
        ));
    }

    private AuditLog audit(String actorEmail, String action, String targetType, String targetId, String detail) {
        return AuditLog.builder().actorEmail(actorEmail).action(action).targetType(targetType).targetId(targetId).detail(detail).build();
    }

    private void normalizePrimaryPathOrder(String slug, int order) {
        onlineCourseRepository.findBySlug(slug)
                .ifPresent(course -> {
                    course.setLearningPathCode(PRIMARY_PATH_CODE);
                    course.setLearningPathName(PRIMARY_PATH_NAME);
                    course.setLearningPathOrder(order);
                    onlineCourseRepository.save(course);
                });
    }

    private void assignCatalogToLongContentManager() {
        userRepository.findByEmail(LONG_CONTENT_MANAGER_EMAIL).ifPresent(manager ->
                onlineCourseRepository.findAll().forEach(course -> {
                    if (course.isPublished()) {
                        course.setCreatedBy(manager);
                    }
                })
        );
    }

    private void seedFoundationCatalogCourses() {
        User manager = userRepository.findByEmail(LONG_CONTENT_MANAGER_EMAIL).orElse(null);
        if (manager == null) return;

        CourseCategory category = courseCategoryRepository.findByCode(CourseCategoryCode.IELTS.name()).orElse(null);
        if (category == null) return;

        upsertFoundationCourse(
                manager, category,
                "ielts-foundation-listening", "IELTS Foundation Listening", 2,
                "Build listening confidence with short IELTS-style practice and guided review.",
                "/course-covers/ielts-listening.png"
        );
        upsertFoundationCourse(
                manager, category,
                "ielts-foundation-speaking", "IELTS Foundation Speaking", 3,
                "Practice clear answers, useful vocabulary, and confident speaking routines.",
                "/course-covers/ielts-speaking.png"
        );
    }

    private void upsertFoundationCourse(
            User manager,
            CourseCategory category,
            String slug,
            String title,
            int pathOrder,
            String description,
            String thumbnailUrl
    ) {
        OnlineCourse course = onlineCourseRepository.findBySlug(slug)
                .orElseGet(() -> OnlineCourse.builder().slug(slug).build());
        course.setCreatedBy(manager);
        course.setTitle(title);
        course.setShortDescription(description);
        course.setDescription(description);
        course.setTargetScore("IELTS Band 5.5");
        course.setDuration("4 weeks");
        course.setStudyMode("Self-paced online");
        course.setPrice(java.math.BigDecimal.ZERO);
        course.setThumbnailUrl(thumbnailUrl);
        course.setStatus(PackageStatus.PUBLISHED);
        course.setFeatured(true);
        course.setDeleted(false);
        course.setCategory(category);
        course.setLevel(CourseLevel.BEGINNER);
        course.setRecommendedCurrentBandMin(3.0);
        course.setTargetBand(5.5);
        course.setLearningPathCode("IELTS_FOUNDATION_REVIEW");
        course.setLearningPathName("Lộ trình IELTS Foundation");
        course.setLearningPathOrder(pathOrder);
        course.setTargetOutcome(description);
        course.setTotalLessons(1);
        course.setTotalHours(4);
        OnlineCourse savedCourse = onlineCourseRepository.save(course);
        course = savedCourse;

        OnlineCourseVersion draftVersion = ensureDraftVersion(course);
        if (draftVersion.getModules().isEmpty()) {
            OnlineCourseModule module = OnlineCourseModule.builder()
                    .title("Foundation study plan")
                    .description("A guided starting module for this learning path.")
                    .sequenceNumber(1)
                    .build();
            module.addLesson(OnlineLesson.builder()
                    .title("Welcome to " + title)
                    .description("Start your foundation study plan.")
                    .contentType("TEXT")
                    .contentText("Follow the study guide and complete the practice activities.")
                    .durationMinutes(30)
                    .sequenceNumber(1)
                    .preview(true)
                    .stableLessonKey("%s-m1-l1".formatted(slug))
                    .build());
            draftVersion.addModule(module);
            onlineCourseVersionRepository.save(draftVersion);
        }
        onlineCourseRepository.save(course);
        onlineCourseVersionService.refreshPublishedSnapshot(course);
    }

    private OnlineCourseVersion ensureDraftVersion(OnlineCourse course) {
        return onlineCourseVersionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.DRAFT)
                .orElseGet(() -> onlineCourseVersionRepository.save(OnlineCourseVersion.builder()
                        .onlineCourse(course)
                        .versionNumber(1)
                        .status(CourseVersionStatus.DRAFT)
                        .contentSnapshotJson("{}")
                        .assessmentIdsJson("[]")
                        .totalRequiredLessons(0)
                        .totalRequiredAssessments(0)
                        .build()));
    }

    private void prepareCatalogAndCertificateDemo(OnlineCourse course) {
        // Keep a second visible path in the public course catalog without changing the two IELTS seed courses.
        course.setLearningPathCode("IELTS_FOUNDATION_REVIEW");
        course.setLearningPathName("Lộ trình IELTS Foundation");
        course.setLearningPathOrder(1);
        course.setRecommendedNextCourseSlug(null);
        onlineCourseRepository.save(course);

        User existing = userRepository.findByEmail(CERTIFICATE_LEARNER_EMAIL).orElse(null);
        if (existing == null) {
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
            userRoleService.assignRole(created, RoleCodes.LEARNER);
            existing = userRepository.save(created);
        } else if (existing.getFullName() == null || existing.getFullName().isBlank() || existing.getFullName().equalsIgnoreCase("Học viên EnglishLab")) {
            existing.setFullName("Học viên Chứng nhận Demo");
            existing = userRepository.save(existing);
        }
        final User learner = existing;

        OnlineCourseEnrollment enrollment = enrollmentRepository.findByStudentAndOnlineCourse(learner, course)
                .orElseGet(() -> enrollmentRepository.save(OnlineCourseEnrollment.builder()
                        .student(learner)
                        .registeredAt(LocalDateTime.now().minusDays(14))
                        .build()));
        enrollment.setStatus(EnrollmentStatus.COMPLETED);
        enrollment.setProgressPercent(100);
        enrollmentRepository.save(enrollment);

        OnlineCourseVersion version = onlineCourseVersionRepository.findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.PUBLISHED)
                .or(() -> onlineCourseVersionRepository.findFirstByOnlineCourseOrderByVersionNumberDesc(course))
                .orElse(null);
        if (version != null && version.getModules() != null) {
            version.getModules().forEach(module -> module.getLessons().forEach(lesson -> completeLesson(learner, enrollment, lesson)));
        }
        enrollForDiscussionDemo(learner, "ielts-master-vocabulary-band-7-plus");
    }

    private void enrollForDiscussionDemo(User learner, String courseSlug) {
        onlineCourseRepository.findBySlug(courseSlug)
                .ifPresent(course -> enrollmentRepository.findByStudentAndOnlineCourse(learner, course)
                        .orElseGet(() -> enrollmentRepository.save(OnlineCourseEnrollment.builder()
                                .student(learner)
                                .status(EnrollmentStatus.ACTIVE)
                                .progressPercent(10)
.registeredAt(LocalDateTime.now().minusDays(3))
                                .build())));
    }

    private void completeLesson(User learner, OnlineCourseEnrollment enrollment, OnlineLesson lesson) {
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
