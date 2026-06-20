package fu.sap490.g23.backend.config;

import fu.sap490.g23.backend.entity.course.CourseCategory;
import fu.sap490.g23.backend.entity.course.enums.CourseCategoryCode;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.enums.PackageStatus;
import fu.sap490.g23.backend.entity.course.PackageType;
import fu.sap490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sap490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.PackageTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OnlineCourseDataSeeder implements CommandLineRunner {

    private static final Set<String> PLACEHOLDER_COURSE_TITLES = Set.of(
            "IELTS Foundation",
            "IELTS Intensive",
            "TOEIC Master",
            "English Communication"
    );

    private final LearningPackageRepository learningPackageRepository;
    private final PackageTypeRepository packageTypeRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final OnlineCourseRepository onlineCourseRepository;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        seedPackageTypes();
        seedCourseCategories();
        backfillMissingCourseCategories();
        cleanupPlaceholderCourses();
    }

    private void seedPackageTypes() {
        seedPackageType(PackageTypeCode.ONLINE_COURSE, "Online Course", "Self-paced or mentor-supported online learning package.");
        seedPackageType(PackageTypeCode.CLASSROOM, "Classroom", "Offline or blended classroom package.");
        seedPackageType(PackageTypeCode.BUNDLE, "Bundle", "A bundle containing multiple learning products.");
        seedPackageType(PackageTypeCode.MOCK_TEST, "Mock Test", "Certification exam simulation package.");
        seedPackageType(PackageTypeCode.SUBSCRIPTION, "Subscription", "Recurring access package.");
    }

    private void seedPackageType(PackageTypeCode code, String name, String description) {
        if (!packageTypeRepository.existsByCode(code)) {
            packageTypeRepository.save(PackageType.builder().code(code).name(name).description(description).build());
        }
    }

    private void seedCourseCategories() {
        seedCategory(CourseCategoryCode.IELTS, "IELTS", "IELTS preparation courses", 1);
        seedCategory(CourseCategoryCode.TOEIC, "TOEIC", "TOEIC preparation courses", 2);
        seedCategory(CourseCategoryCode.COMMUNICATION, "Giao tiếp", "English communication courses", 3);
        seedCategory(CourseCategoryCode.FOUNDATION, "Mất gốc", "Foundation English courses", 4);
        seedCategory(CourseCategoryCode.ONLINE, "Online", "Online-first learning programs", 5);
    }

    private void seedCategory(CourseCategoryCode code, String name, String description, int order) {
        if (!courseCategoryRepository.existsByCode(code)) {
            courseCategoryRepository.save(CourseCategory.builder()
                    .code(code)
                    .name(name)
                    .description(description)
                    .displayOrder(order)
                    .build());
        }
    }

    private void backfillMissingCourseCategories() {
        CourseCategory defaultCategory = courseCategoryRepository.findByCode(CourseCategoryCode.ONLINE)
                .orElseThrow(() -> new IllegalStateException("ONLINE course category is missing"));

        for (OnlineCourse course : onlineCourseRepository.findAllByCategoryIsNull()) {
            course.setCategory(resolveCategory(course, defaultCategory));
        }
    }

    private CourseCategory resolveCategory(OnlineCourse course, CourseCategory defaultCategory) {
        String normalized = (course.getLearningPackage().getTitle() + " " + course.getLearningPackage().getSlug())
                .toLowerCase(Locale.ROOT);
        if (normalized.contains("ielts")) {
            return findCategory(CourseCategoryCode.IELTS, defaultCategory);
        }
        if (normalized.contains("toeic")) {
            return findCategory(CourseCategoryCode.TOEIC, defaultCategory);
        }
        if (normalized.contains("communication")) {
            return findCategory(CourseCategoryCode.COMMUNICATION, defaultCategory);
        }
        if (normalized.contains("foundation")) {
            return findCategory(CourseCategoryCode.FOUNDATION, defaultCategory);
        }
        return defaultCategory;
    }

    private CourseCategory findCategory(CourseCategoryCode code, CourseCategory fallback) {
        return courseCategoryRepository.findByCode(code).orElse(fallback);
    }

    private void cleanupPlaceholderCourses() {
        learningPackageRepository.findAll().stream()
                .filter(learningPackage -> PLACEHOLDER_COURSE_TITLES.contains(learningPackage.getTitle()))
                .forEach(learningPackage -> {
                    learningPackage.setDeleted(true);
                    learningPackage.setFeatured(false);
                    learningPackage.setStatus(PackageStatus.DRAFT);
                });
    }
}
