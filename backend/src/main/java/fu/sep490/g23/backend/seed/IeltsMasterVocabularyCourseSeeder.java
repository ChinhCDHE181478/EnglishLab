package fu.sep490.g23.backend.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.core.JsonProcessingException;
import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.entity.course.CourseLessonFlashcardRef;
import fu.sep490.g23.backend.entity.course.enums.CourseCategoryCode;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import fu.sep490.g23.backend.entity.curriculum.FlashcardSet;
import fu.sep490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.repository.curriculum.ContentBankItemRepository;
import fu.sep490.g23.backend.repository.curriculum.FlashcardSetRepository;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
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

    private final CourseCategoryRepository courseCategoryRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseVersionRepository onlineCourseVersionRepository;
    private final FlashcardSetRepository flashcardSetRepository;
    private final ContentBankItemRepository contentBankItemRepository;
    private final OnlineCourseVersionService onlineCourseVersionService;

    @Value("${app.seed.test.enabled:false}")
    private boolean seedEnabled;

    @Value("${app.seed.sheet.enabled:false}")
    private boolean sheetSeedEnabled;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (!seedEnabled && !sheetSeedEnabled && onlineCourseRepository.existsBySlug("ielts-master-vocabulary-band-7-plus")) {
            return;
        }

        CourseSeed seed = OBJECT_MAPPER
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .readValue(new ClassPathResource(SEED_PATH).getInputStream(), CourseSeed.class);

        CourseCategory category = courseCategoryRepository.findByCode(CourseCategoryCode.IELTS.name())
                .orElseGet(() -> courseCategoryRepository.save(CourseCategory.builder()
                        .code(CourseCategoryCode.IELTS.name())
                        .name("IELTS")
                        .description("IELTS exam preparation courses")
                        .displayOrder(1)
                        .active(true)
                        .build()));

        OnlineCourse onlineCourse = onlineCourseRepository.findBySlug(seed.slug())
                .orElseGet(() -> OnlineCourse.builder()
                        .slug(seed.slug())
                        .build());

        onlineCourse.setTitle(seed.title());
        onlineCourse.setShortDescription(seed.description());
        onlineCourse.setDescription(seed.description());
        onlineCourse.setTargetScore("IELTS Band 7+");
        onlineCourse.setDuration(seed.totalDurationText() == null ? "8 giờ 9 phút" : seed.totalDurationText());
        onlineCourse.setStudyMode("Tự học online theo playlist IELTS Master");
        onlineCourse.setPrice(paidPrice(seed.price()));
        onlineCourse.setThumbnailUrl(seed.thumbnail());
        onlineCourse.setStatus(PackageStatus.PUBLISHED);
        onlineCourse.setDisplayOrder(6);
        onlineCourse.setFeatured(true);
        onlineCourse.setDeleted(false);
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
        OnlineCourse savedOnlineCourse = onlineCourseRepository.save(onlineCourse);
        onlineCourse = savedOnlineCourse;

        for (ModuleSeed moduleSeed : seed.modules()) {
            upsertModule(onlineCourse, seed.slug(), moduleSeed);
        }

        OnlineCourseVersion draftVersion = ensureDraftVersion(onlineCourse);
        draftVersion.getModules().sort(Comparator.comparing(OnlineCourseModule::getDisplayOrder).thenComparing(module -> module.getId() == null ? Long.MAX_VALUE : module.getId()));
        onlineCourseVersionRepository.save(draftVersion);
        onlineCourseRepository.save(onlineCourse);
        onlineCourseVersionService.refreshPublishedSnapshot(onlineCourse);
    }

    private void upsertModule(OnlineCourse onlineCourse, String courseSlug, ModuleSeed moduleSeed) {
        OnlineCourseVersion draftVersion = ensureDraftVersion(onlineCourse);
        OnlineCourseModule module = draftVersion.getModules().stream()
                .filter(existingModule -> existingModule.getDisplayOrder() != null && existingModule.getDisplayOrder().equals(moduleSeed.order()))
                .findFirst()
                .orElseGet(() -> {
                    OnlineCourseModule newModule = new OnlineCourseModule();
                    draftVersion.addModule(newModule);
                    return newModule;
                });

        module.setTitle(moduleSeed.title());
        module.setDescription(moduleSeed.description());
        module.setDisplayOrder(moduleSeed.order());

        for (LessonSeed lessonSeed : moduleSeed.lessons()) {
            upsertLesson(module, courseSlug, lessonSeed);
        }

        module.getLessons().sort(Comparator.comparing(OnlineLesson::getDisplayOrder).thenComparing(lesson -> lesson.getId() == null ? Long.MAX_VALUE : lesson.getId()));
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

    private void upsertLesson(OnlineCourseModule module, String courseSlug, LessonSeed lessonSeed) {
        OnlineLesson lesson = module.getLessons().stream()
                .filter(existingLesson -> existingLesson.getDisplayOrder() != null && existingLesson.getDisplayOrder().equals(lessonSeed.order()))
                .findFirst()
                .orElseGet(() -> {
                    OnlineLesson newLesson = new OnlineLesson();
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
        int moduleOrder = module.getDisplayOrder() == null ? 0 : module.getDisplayOrder();
        lesson.setLessonKey("%s-m%d-l%d".formatted(courseSlug, moduleOrder, lessonSeed.order()));

        upsertFlashcardSetForVocabularyLesson(module, lesson);
    }

    private void upsertFlashcardSetForVocabularyLesson(OnlineCourseModule module, OnlineLesson lesson) {
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
                .anyMatch(ref -> ref.getContentBankItem() != null && savedSet.getId() != null
                        && savedSet.getId().equals(ref.getContentBankItem().getId()));
        if (!alreadyLinked) {
            ContentBankItem bankItem = contentBankItemRepository.findById(savedSet.getId())
                    .orElseThrow(() -> new IllegalStateException("Flashcard bank item missing after save"));
            lesson.addFlashcardRef(CourseLessonFlashcardRef.builder()
                    .contentBankItem(bankItem)
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

    private BigDecimal paidPrice(BigDecimal price) {
        if (price == null || price.signum() <= 0) {
            return BigDecimal.valueOf(1_290_000);
        }
        return price;
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
