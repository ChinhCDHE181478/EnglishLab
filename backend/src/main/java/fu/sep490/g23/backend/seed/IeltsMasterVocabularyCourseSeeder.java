package fu.sep490.g23.backend.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.core.JsonProcessingException;
import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.entity.course.CourseLessonFlashcardRef;
import fu.sep490.g23.backend.entity.course.enums.CourseCategoryCode;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.CourseModule;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.Lesson;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.PackageType;
import fu.sep490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sep490.g23.backend.entity.curriculum.FlashcardSet;
import fu.sep490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sep490.g23.backend.repository.course.LearningPackageRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.PackageTypeRepository;
import fu.sep490.g23.backend.repository.curriculum.FlashcardSetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(41)
@RequiredArgsConstructor
public class IeltsMasterVocabularyCourseSeeder implements CommandLineRunner {

    private static final String SEED_PATH = "course-seeds/ielts_master_vocabulary_complete_course.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern VOCAB_ENTRY_PATTERN = Pattern.compile(
            "###\\s+\\d+\\.\\s+(.+?)\\R\\*\\*Meaning:\\*\\*\\s+(.+?)\\R\\R\\*\\*IELTS example:\\*\\*\\s+(.+?)\\R\\R\\*\\*Common error to avoid:\\*\\*\\s+(.+?)(?=\\R\\R###|\\R\\R##|\\z)",
            Pattern.DOTALL
    );

    private final PackageTypeRepository packageTypeRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final FlashcardSetRepository flashcardSetRepository;

    @Value("${app.seed.test.enabled:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!seedEnabled) {
            return;
        }

        CourseSeed seed = OBJECT_MAPPER
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(new ClassPathResource(SEED_PATH).getInputStream(), CourseSeed.class);

        PackageType packageType = packageTypeRepository.findByCode(PackageTypeCode.ONLINE_COURSE)
                .orElseGet(() -> packageTypeRepository.save(PackageType.builder()
                        .code(PackageTypeCode.ONLINE_COURSE)
                        .name("Online Course")
                        .description("Self-paced online learning package")
                        .active(true)
                        .build()));

        CourseCategory category = courseCategoryRepository.findByCode(CourseCategoryCode.IELTS.name())
                .orElseGet(() -> courseCategoryRepository.save(CourseCategory.builder()
                        .code(CourseCategoryCode.IELTS.name())
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
        onlineCourse.setRecommendedCurrentBandMin(5.5);
        onlineCourse.setTargetBand(7.0);
        onlineCourse.setLearningPathCode("IELTS_BAND_55_TO_70");
        onlineCourse.setLearningPathName("IELTS 5.5 to 7.0 Self-Paced Path");
        onlineCourse.setLearningPathOrder(1);
        onlineCourse.setTargetOutcome("Use band-7 topic vocabulary, collocations, and examples naturally in IELTS Writing and Speaking responses.");
        onlineCourse.setRecommendedNextCourseSlug("e2-ielts-practice-tests");
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

        upsertFlashcardSetForVocabularyLesson(module, lesson);
    }

    private void upsertFlashcardSetForVocabularyLesson(CourseModule module, Lesson lesson) {
        if (lesson.getTitle() == null || !lesson.getTitle().contains("Vocabulary Bank and Model Usage")) {
            return;
        }
        List<Map<String, String>> cards = extractFlashcards(lesson.getContentText());
        if (cards.isEmpty()) {
            return;
        }

        String title = lesson.getTitle();
        FlashcardSet set = flashcardSetRepository.findByTitleIgnoreCase(title)
                .orElseGet(() -> FlashcardSet.builder()
                        .title(title)
                        .build());
        set.setTitle(title);
        set.setDescription("Bộ thẻ từ vựng dùng chung cho " + module.getTitle() + ".");
        set.setExamCategory("IELTS");
        set.setSkill("VOCABULARY");
        set.setTags(module.getTitle() + ", IELTS vocabulary, course-linked");
        set.setStatus("PUBLISHED");
        set.setDisplayOrder(module.getDisplayOrder() == null ? 0 : module.getDisplayOrder());
        set.setCardsJson(toJson(cards));
        FlashcardSet savedSet = flashcardSetRepository.save(set);

        boolean alreadyLinked = lesson.getFlashcardRefs().stream()
                .anyMatch(ref -> ref.getFlashcardSet() != null && savedSet.getId() != null
                        && savedSet.getId().equals(ref.getFlashcardSet().getId()));
        if (!alreadyLinked) {
            lesson.addFlashcardRef(CourseLessonFlashcardRef.builder()
                    .flashcardSet(savedSet)
                    .displayOrder(lesson.getFlashcardRefs().size() + 1)
                    .build());
        }
    }

    private List<Map<String, String>> extractFlashcards(String contentText) {
        List<Map<String, String>> cards = new ArrayList<>();
        if (contentText == null || contentText.isBlank()) {
            return cards;
        }
        Matcher matcher = VOCAB_ENTRY_PATTERN.matcher(contentText);
        while (matcher.find()) {
            Map<String, String> card = new LinkedHashMap<>();
            card.put("front", cleanMarkdownValue(matcher.group(1)));
            card.put("back", cleanMarkdownValue(matcher.group(2)));
            card.put("example", cleanMarkdownValue(matcher.group(3)));
            card.put("commonMistake", cleanMarkdownValue(matcher.group(4)));
            cards.add(card);
        }
        return cards;
    }

    private String cleanMarkdownValue(String value) {
        return value == null ? "" : value
                .replace("\r", "")
                .replaceAll("\\n+", " ")
                .trim();
    }

    private String toJson(List<Map<String, String>> cards) {
        try {
            return OBJECT_MAPPER.writeValueAsString(cards);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Không thể tạo dữ liệu flashcard từ seed IELTS Vocabulary.", ex);
        }
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
