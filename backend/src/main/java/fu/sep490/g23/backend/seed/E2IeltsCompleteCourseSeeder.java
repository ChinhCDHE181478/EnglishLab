package fu.sep490.g23.backend.seed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.response.course.TranscriptSegmentResponse;
import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.entity.course.enums.CourseCategoryCode;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.CourseModule;
import fu.sep490.g23.backend.entity.course.LearningPackage;
import fu.sep490.g23.backend.entity.course.Lesson;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.PackageType;
import fu.sep490.g23.backend.entity.course.enums.PackageTypeCode;
import fu.sep490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sep490.g23.backend.repository.course.LearningPackageRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.PackageTypeRepository;
import fu.sep490.g23.backend.service.course.YouTubeTranscriptService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
@Order(40)
@RequiredArgsConstructor
@Slf4j
public class E2IeltsCompleteCourseSeeder implements CommandLineRunner {

    private static final String COURSE_SLUG = "e2-ielts-practice-tests";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PackageTypeRepository packageTypeRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final LearningPackageRepository learningPackageRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final YouTubeTranscriptService youTubeTranscriptService;

    @Value("${app.seed.test.enabled:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

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

        LearningPackage learningPackage = learningPackageRepository.findBySlugAndDeletedFalse(COURSE_SLUG)
                .orElseGet(() -> LearningPackage.builder()
                        .slug(COURSE_SLUG)
                        .packageType(packageType)
                        .build());

        learningPackage.setPackageType(packageType);
        learningPackage.setTitle("E2 IELTS Practice Tests");
        learningPackage.setShortDescription("IELTS practice course curated from public E2 IELTS YouTube videos.");
        learningPackage.setDescription("An IELTS practice course for Listening, Reading, and Speaking practice. Each video is organized as one module with a study guide, the original video lesson, and follow-up practice.");
        learningPackage.setTargetScore("IELTS 5.5 - 7.0");
        learningPackage.setDuration("5 hours 32 minutes");
        learningPackage.setStudyMode("Self-paced online video course");
        learningPackage.setPrice(BigDecimal.valueOf(10000));
        learningPackage.setThumbnailUrl("https://i.ytimg.com/vi/v3axTdVoYkY/hqdefault.jpg");
        learningPackage.setStatus(PackageStatus.PUBLISHED);
        learningPackage.setDisplayOrder(5);
        learningPackage.setFeatured(true);
        learningPackage.setDeleted(false);
        learningPackage = learningPackageRepository.save(learningPackage);
        LearningPackage savedPackage = learningPackage;

        OnlineCourse onlineCourse = onlineCourseRepository.findByLearningPackage(savedPackage)
                .orElseGet(() -> OnlineCourse.builder()
                        .learningPackage(savedPackage)
                        .build());

        onlineCourse.setLearningPackage(savedPackage);
        onlineCourse.setCategory(category);
        onlineCourse.setLevel(CourseLevel.INTERMEDIATE);
        onlineCourse.setRecommendedCurrentBandMin(6.0);
        onlineCourse.setTargetBand(7.0);
        onlineCourse.setLearningPathCode("IELTS_BAND_55_TO_70");
        onlineCourse.setLearningPathName("IELTS 5.5 to 7.0 Self-Paced Path");
        onlineCourse.setLearningPathOrder(2);
        onlineCourse.setTargetOutcome("Complete IELTS-style practice tests, analyze mistakes, and build a personal review plan before the final mock.");
        onlineCourse.setRecommendedNextCourseSlug(null);
        onlineCourse.setTotalLessons(18);
        onlineCourse.setTotalHours(6);

        addModule(onlineCourse, 1,
                "IELTS Listening Practice Test with Answers",
                "Practice a full Listening test and focus on main ideas, details, and answer checking.",
                "Listening",
                "https://www.youtube.com/watch?v=v3axTdVoYkY",
                29,
                true,
                "Review the Listening test format and a quick question-reading strategy before you start.",
                "Log mistakes by type: keyword, synonym, number, spelling, and distractor.");

        addModule(onlineCourse, 2,
                "IELTS Reading Practice Test with Answer Explanations",
                "Reading practice with answer explanations for scanning, skimming, and locating evidence.",
                "Reading",
                "https://www.youtube.com/watch?v=kCthrwUz68w",
                26,
                false,
                "Review how to identify keywords and predict where evidence appears in the passage.",
                "Create a short review sheet with keywords, paraphrases, and the reason each answer is correct.");

        addModule(onlineCourse, 3,
                "Full IELTS Listening Test with Answers | 2024",
                "A full Listening test to build pacing and time control under test-like conditions.",
                "Listening",
                "https://www.youtube.com/watch?v=VUtUOTrJ2Kk",
                33,
                false,
                "Prepare an answer sheet and complete the test in one pass without pausing the video.",
                "Score your work, replay difficult segments, and write short transcripts for the hardest questions.");

        addModule(onlineCourse, 4,
                "IELTS Speaking Practice Test with Answers",
                "A simulated Speaking test to improve structure, examples, and natural delivery.",
                "Speaking",
                "https://www.youtube.com/watch?v=L520xwhFGiI",
                33,
                false,
                "Review Fluency, Lexical Resource, Grammar Range, and Pronunciation before watching.",
                "Record your own answers for three prompts and self-assess with the IELTS criteria.");

        addModule(onlineCourse, 5,
                "IELTS Listening: Techniques and Practice Questions",
                "Learn core Listening techniques and apply them in guided practice questions.",
                "Listening",
                "https://www.youtube.com/watch?v=6fk6W7Knld8",
                36,
                false,
                "Focus on predicting, signposting, paraphrasing, and avoiding distractors.",
                "Collect ten useful keywords or paraphrases and turn them into a personal strategy note.");

        addModule(onlineCourse, 6,
                "100 IELTS Speaking Questions | Part 1 - 20+ IELTS Speaking Topics",
                "A speaking prompt bank across common Part 1 topics to build faster response habits.",
                "Speaking",
                "https://www.youtube.com/watch?v=OTjzR2QCc_E",
                35,
                false,
                "Choose five familiar topics and outline short answers with the Answer-Explain-Example pattern.",
                "Build a personal speaking bank with twenty questions, idea prompts, and strong vocabulary.");

        backfillMissingVideoTranscripts(onlineCourse);
        onlineCourseRepository.save(onlineCourse);
    }

    private void backfillMissingVideoTranscripts(OnlineCourse onlineCourse) {
        onlineCourse.getModules().stream()
                .flatMap(module -> module.getLessons().stream())
                .filter(lesson -> lesson.getVideoUrl() != null && !lesson.getVideoUrl().isBlank())
                .filter(lesson -> lesson.getTranscriptSegmentsJson() == null || lesson.getTranscriptSegmentsJson().isBlank())
                .forEach(lesson -> {
                    List<TranscriptSegmentResponse> segments =
                            youTubeTranscriptService.fetchTranscriptSegments(lesson.getVideoUrl());
                    if (segments.isEmpty()) {
                        log.warn("Không tìm thấy caption để backfill cho bài học E2: {}", lesson.getTitle());
                        return;
                    }
                    try {
                        lesson.setTranscriptSegmentsJson(OBJECT_MAPPER.writeValueAsString(segments));
                        log.info("Đã backfill {} đoạn transcript cho bài học E2: {}", segments.size(), lesson.getTitle());
                    } catch (JsonProcessingException ex) {
                        log.warn("Không thể lưu transcript cho bài học E2 {}: {}", lesson.getTitle(), ex.getMessage());
                    }
                });
    }

    private void addModule(
            OnlineCourse onlineCourse,
            int order,
            String videoTitle,
            String moduleDescription,
            String skill,
            String videoUrl,
            int videoDurationMinutes,
            boolean preview,
            String preLessonDescription,
            String postLessonDescription
    ) {
        CourseModule module = findModule(onlineCourse, order);
        module.setTitle("Module " + order + ": " + videoTitle);
        module.setDescription(moduleDescription);
        module.setDisplayOrder(order);

        upsertLesson(
                module,
                1,
                "Lesson " + order + ".1: Goals and strategy for " + skill,
                preLessonDescription,
                buildTextLessonContent(order, 1, "Mục tiêu và chiến lược cho " + skill, preLessonDescription),
                null,
                10,
                preview
        );

        upsertLesson(
                module,
                2,
                "Lesson " + order + ".2: Video practice - " + videoTitle,
                "Watch the original E2 IELTS video and track mistakes while following the guided practice flow.",
                buildVideoLessonContent(order, videoTitle),
                videoUrl,
                videoDurationMinutes,
                preview
        );

        upsertLesson(
                module,
                3,
                "Lesson " + order + ".3: Review and post-video practice",
                postLessonDescription,
                buildTextLessonContent(order, 3, "Ôn tập sau video", postLessonDescription),
                null,
                15,
                false
        );

        module.getLessons().sort(Comparator.comparing(Lesson::getDisplayOrder).thenComparing(lesson -> lesson.getId() == null ? Long.MAX_VALUE : lesson.getId()));
        onlineCourse.getModules().sort(Comparator.comparing(CourseModule::getDisplayOrder).thenComparing(moduleItem -> moduleItem.getId() == null ? Long.MAX_VALUE : moduleItem.getId()));
    }

    private CourseModule findModule(OnlineCourse onlineCourse, int order) {
        return onlineCourse.getModules().stream()
                .filter(module -> module.getDisplayOrder() != null && module.getDisplayOrder() == order)
                .findFirst()
                .orElseGet(() -> {
                    CourseModule module = new CourseModule();
                    onlineCourse.addModule(module);
                    return module;
                });
    }

    private void upsertLesson(CourseModule module, int order, String title, String description, String contentText, String videoUrl, int durationMinutes, boolean preview) {
        Lesson lesson = module.getLessons().stream()
                .filter(existingLesson -> existingLesson.getDisplayOrder() != null && existingLesson.getDisplayOrder() == order)
                .findFirst()
                .orElseGet(() -> {
                    Lesson newLesson = new Lesson();
                    module.addLesson(newLesson);
                    return newLesson;
                });

        lesson.setTitle(title);
        lesson.setDescription(description);
        lesson.setContentType(videoUrl == null ? "text" : "video");
        lesson.setContentText(contentText);
        lesson.setVideoUrl(videoUrl);
        lesson.setMaterialUrl(null);
        lesson.setDurationMinutes(durationMinutes);
        lesson.setDisplayOrder(order);
        lesson.setPreview(preview);
    }

    private String buildTextLessonContent(int moduleOrder, int lessonOrder, String heading, String description) {
        return """
                ## Lesson %d.%d: %s

                %s

                ### Việc cần làm
                - Đọc kỹ mục tiêu của bước học này trước khi bắt đầu.
                - Ghi lại 3-5 ý chính hoặc lỗi quan trọng cần lưu ý trong quá trình học.
                - Sau khi hoàn thành, đánh dấu bài học để mở bước tiếp theo của mô-đun.
                """.formatted(moduleOrder, lessonOrder, heading, description);
    }

    private String buildVideoLessonContent(int moduleOrder, String videoTitle) {
        return """
                ## Lesson %d.2: Video practice - %s

                Xem video theo đúng tiến độ bài học và ghi lại lỗi hoặc chiến thuật làm bài hữu ích cho bản thân.

                ### Cách học với video
                - Xem video một lượt như bài thi thật, chỉ tạm dừng khi thật sự cần ghi chú.
                - Ghi lại câu sai, từ khóa bỏ lỡ, dấu hiệu nhiễu và mẹo xử lý được nhắc trong video.
                - Sau khi xem xong, tự tóm tắt 2-3 điều bạn cần luyện lại trước khi sang bước ôn tập.
                """.formatted(moduleOrder, videoTitle);
    }
}
