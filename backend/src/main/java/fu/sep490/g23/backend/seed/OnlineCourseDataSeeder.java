package fu.sep490.g23.backend.seed;

import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.entity.course.enums.CourseCategoryCode;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.PackageType;
import fu.sep490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sep490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sep490.g23.backend.repository.course.LearningPackageRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.PackageTypeRepository;
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

    @Value("${app.seed.test.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.seed.sheet.enabled:false}")
    private boolean sheetEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        repairLegacyCourseCategories();
        if (seedEnabled || sheetEnabled) {
            seedPackageTypes();
            seedCourseCategories();
            backfillMissingCourseCategories();
        }
        if (!seedEnabled) {
            return;
        }
        cleanupPlaceholderCourses();
    }

    private void seedPackageTypes() {
        seedPackageType(PackageTypeCode.ONLINE_COURSE, "Online Course", "Self-paced or mentor-supported online learning package.");
        seedPackageType(PackageTypeCode.CLASSROOM, "Classroom", "Offline or blended classroom package.");
        seedPackageType(PackageTypeCode.BUNDLE, "Bundle", "Combined learning package.");
        seedPackageType(PackageTypeCode.MOCK_TEST, "Mock Test", "Certification exam simulation package.");
        seedPackageType(PackageTypeCode.SUBSCRIPTION, "Subscription", "Recurring access package.");
    }

    private void seedPackageType(PackageTypeCode code, String name, String description) {
        if (!packageTypeRepository.existsByCode(code)) {
            packageTypeRepository.save(PackageType.builder().code(code).name(name).description(description).build());
        }
    }

    private void seedCourseCategories() {
        seedCategory(CourseCategoryCode.IELTS, "IELTS", "Khóa học luyện thi IELTS.", 1);
        seedCategory(CourseCategoryCode.TOEIC, "TOEIC", "Khóa học luyện thi TOEIC.", 2);
        seedCategory(CourseCategoryCode.COMMUNICATION, "Giao tiếp", "Khóa học tiếng Anh giao tiếp.", 3);
        seedCategory(CourseCategoryCode.FOUNDATION, "Nền tảng", "Khóa học củng cố nền tảng tiếng Anh.", 4);
        seedCategory(CourseCategoryCode.ONLINE, "Trực tuyến", "Chương trình học trực tuyến linh hoạt.", 5);
    }

    private void repairLegacyCourseCategories() {
        repairCategoryIfPresent(CourseCategoryCode.IELTS, "IELTS", "Khóa học luyện thi IELTS.");
        repairCategoryIfPresent(CourseCategoryCode.TOEIC, "TOEIC", "Khóa học luyện thi TOEIC.");
        repairCategoryIfPresent(CourseCategoryCode.COMMUNICATION, "Giao tiếp", "Khóa học tiếng Anh giao tiếp.");
        repairCategoryIfPresent(CourseCategoryCode.FOUNDATION, "Nền tảng", "Khóa học củng cố nền tảng tiếng Anh.");
        repairCategoryIfPresent(CourseCategoryCode.ONLINE, "Trực tuyến", "Chương trình học trực tuyến linh hoạt.");
    }

    private void repairCategoryIfPresent(CourseCategoryCode code, String name, String description) {
        courseCategoryRepository.findByCode(code.name()).ifPresent(category -> {
            boolean changed = false;
            if (shouldRepairCategoryName(code, category.getName())) {
                category.setName(name);
                changed = true;
            }
            if (shouldRepairCategoryDescription(category.getDescription())) {
                category.setDescription(description);
                changed = true;
            }
            if (changed) {
                courseCategoryRepository.save(category);
            }
        });
    }

    private void seedCategory(CourseCategoryCode code, String name, String description, int order) {
        courseCategoryRepository.findByCode(code.name()).ifPresentOrElse(category -> {
            repairCategoryIfPresent(code, name, description);
        }, () -> courseCategoryRepository.save(CourseCategory.builder()
                    .code(code.name())
                    .name(name)
                    .description(description)
                    .displayOrder(order)
                    .build()));
    }

    private boolean shouldRepairCategoryName(CourseCategoryCode code, String value) {
        if (value == null || value.isBlank() || containsMojibake(value)) {
            return true;
        }
        return (code == CourseCategoryCode.FOUNDATION && value.equals("Mất gốc"))
                || (code == CourseCategoryCode.ONLINE && value.equalsIgnoreCase("Online"));
    }

    private boolean shouldRepairCategoryDescription(String value) {
        if (value == null || value.isBlank() || containsMojibake(value)) {
            return true;
        }
        return value.equals("IELTS preparation courses")
                || value.equals("TOEIC preparation courses")
                || value.equals("English communication courses")
                || value.equals("Foundation English courses")
                || value.equals("Online-first learning programs");
    }

    private boolean containsMojibake(String value) {
        return value.contains("\u00C3")
                || value.contains("\u00C4")
                || value.contains("\u00C2")
                || value.contains("\u00E1\u00BB")
                || value.contains("\u00E1\u00BA");
    }

    private void backfillMissingCourseCategories() {
        CourseCategory defaultCategory = courseCategoryRepository.findByCode(CourseCategoryCode.ONLINE.name())
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
        return courseCategoryRepository.findByCode(code.name()).orElse(fallback);
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
