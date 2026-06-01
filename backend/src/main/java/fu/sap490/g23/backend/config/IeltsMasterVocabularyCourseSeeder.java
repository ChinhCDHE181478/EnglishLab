package fu.sap490.g23.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import fu.sap490.g23.backend.entity.course.CourseCategory;
import fu.sap490.g23.backend.entity.course.CourseCategoryCode;
import fu.sap490.g23.backend.entity.course.CourseLevel;
import fu.sap490.g23.backend.entity.course.CourseModule;
import fu.sap490.g23.backend.entity.course.LearningPackage;
import fu.sap490.g23.backend.entity.course.Lesson;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageStatus;
import fu.sap490.g23.backend.entity.course.PackageType;
import fu.sap490.g23.backend.entity.course.PackageTypeCode;
import fu.sap490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sap490.g23.backend.repository.course.LearningPackageRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.PackageTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
@Order(41)
@RequiredArgsConstructor
public class IeltsMasterVocabularyCourseSeeder implements CommandLineRunner {

    private static final String SEED_PATH = "course-seeds/ielts_master_vocabulary_complete_course.json";

    private final PackageTypeRepository packageTypeRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final OnlineCourseRepository onlineCourseRepository;

    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!seedEnabled) {
            return;
        }

        CourseSeed seed = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(new ClassPathResource(SEED_PATH).getInputStream(), CourseSeed.class);

        PackageType packageType = packageTypeRepository.findByCode(PackageTypeCode.ONLINE_COURSE)
                .orElseGet(() -> packageTypeRepository.save(PackageType.builder()
                        .code(PackageTypeCode.ONLINE_COURSE)
                        .name("Online Course")
                        .description("Self-paced online learning package")
                        .active(true)
                        .build()));

        CourseCategory category = courseCategoryRepository.findByCode(CourseCategoryCode.IELTS)
                .orElseGet(() -> courseCategoryRepository.save(CourseCategory.builder()
                        .code(CourseCategoryCode.IELTS)
                        .name("IELTS")
                        .description("IELTS exam preparation courses")
                        .displayOrder(1)
                        .active(true)
                        .build()));

        LearningPackage learningPackage = learningPackageRepository.findBySlugAndDeletedFalse(seed.slug())
                .orElseGet(() -> LearningPackage.builder()
                        .slug(seed.slug())
                        .packageType(packageType)
                        .build());

        learningPackage.setPackageType(packageType);
        learningPackage.setTitle(seed.title());
        learningPackage.setShortDescription(seed.description());
        learningPackage.setDescription(seed.description());
        learningPackage.setTargetScore("IELTS Band 7+");
        learningPackage.setDuration(seed.totalDurationText() == null ? "8 giờ 9 phút" : seed.totalDurationText());
        learningPackage.setStudyMode("Tự học online theo playlist IELTS Master");
        learningPackage.setPrice(seed.price() == null ? BigDecimal.ZERO : seed.price());
        learningPackage.setThumbnailUrl(seed.thumbnail());
        learningPackage.setStatus(PackageStatus.PUBLISHED);
        learningPackage.setDisplayOrder(6);
        learningPackage.setFeatured(true);
        learningPackage.setDeleted(false);
        LearningPackage savedPackage = learningPackageRepository.save(learningPackage);

        OnlineCourse onlineCourse = onlineCourseRepository.findByLearningPackage(savedPackage)
                .orElseGet(() -> OnlineCourse.builder()
                        .learningPackage(savedPackage)
                        .build());

        onlineCourse.setLearningPackage(savedPackage);
        onlineCourse.setCategory(category);
        onlineCourse.setLevel(CourseLevel.ADVANCED);
        onlineCourse.setTotalLessons(seed.totalLessons() == null ? countLessons(seed.modules()) : seed.totalLessons());
        onlineCourse.setTotalHours(seed.totalDurationSeconds() == null ? 8 : Math.max(1, (int) Math.ceil(seed.totalDurationSeconds() / 3600.0)));

        for (ModuleSeed moduleSeed : seed.modules()) {
            upsertModule(onlineCourse, moduleSeed);
        }

        onlineCourse.getModules().sort(Comparator.comparing(CourseModule::getDisplayOrder).thenComparing(module -> module.getId() == null ? Long.MAX_VALUE : module.getId()));
        onlineCourseRepository.save(onlineCourse);
    }

    private void upsertModule(OnlineCourse onlineCourse, ModuleSeed moduleSeed) {
        CourseModule module = onlineCourse.getModules().stream()
                .filter(existingModule -> existingModule.getDisplayOrder() != null && existingModule.getDisplayOrder().equals(moduleSeed.order()))
                .findFirst()
                .orElseGet(() -> {
                    CourseModule newModule = new CourseModule();
                    onlineCourse.addModule(newModule);
                    return newModule;
                });

        module.setTitle(moduleSeed.title());
        module.setDescription(moduleSeed.description());
        module.setDisplayOrder(moduleSeed.order());

        for (LessonSeed lessonSeed : moduleSeed.lessons()) {
            upsertLesson(module, lessonSeed);
        }

        module.getLessons().sort(Comparator.comparing(Lesson::getDisplayOrder).thenComparing(lesson -> lesson.getId() == null ? Long.MAX_VALUE : lesson.getId()));
    }

    private void upsertLesson(CourseModule module, LessonSeed lessonSeed) {
        Lesson lesson = module.getLessons().stream()
                .filter(existingLesson -> existingLesson.getDisplayOrder() != null && existingLesson.getDisplayOrder().equals(lessonSeed.order()))
                .findFirst()
                .orElseGet(() -> {
                    Lesson newLesson = new Lesson();
                    module.addLesson(newLesson);
                    return newLesson;
                });

        lesson.setTitle(lessonSeed.title());
        lesson.setDescription(lessonSeed.description());
        lesson.setContentType(lessonSeed.contentType());
        lesson.setContentText(lessonSeed.contentText());
        lesson.setVideoUrl(lessonSeed.videoUrl());
        lesson.setMaterialUrl(null);
        lesson.setDurationMinutes(toMinutes(lessonSeed.durationSeconds()));
        lesson.setDisplayOrder(lessonSeed.order());
        lesson.setPreview(Boolean.TRUE.equals(lessonSeed.isPreview()));
    }

    private int toMinutes(Integer seconds) {
        return seconds == null ? 0 : Math.max(1, (int) Math.ceil(seconds / 60.0));
    }

    private int countLessons(List<ModuleSeed> modules) {
        return modules == null ? 0 : modules.stream().mapToInt(module -> module.lessons() == null ? 0 : module.lessons().size()).sum();
    }

    private record CourseSeed(
            String title,
            String slug,
            String description,
            BigDecimal price,
            String thumbnail,
            Integer totalLessons,
            Integer totalDurationSeconds,
            String totalDurationText,
            List<ModuleSeed> modules
    ) {
    }

    private record ModuleSeed(
            String title,
            String description,
            Integer order,
            List<LessonSeed> lessons
    ) {
    }

    private record LessonSeed(
            String title,
            String description,
            String contentType,
            String contentText,
            String videoUrl,
            Integer durationSeconds,
            Integer order,
            Boolean isPreview
    ) {
    }
}
