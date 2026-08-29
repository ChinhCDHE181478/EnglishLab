package fu.sep490.g23.backend.seed;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.response.course.TranscriptSegmentResponse;
import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.entity.course.enums.CourseCategoryCode;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
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
    private static final String BUNNY_LIBRARY_ID = "729032";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CourseCategoryRepository courseCategoryRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseVersionRepository onlineCourseVersionRepository;
    private final YouTubeTranscriptService youTubeTranscriptService;
    private final OnlineCourseVersionService onlineCourseVersionService;

    @Value("${app.seed.test.enabled:false}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (onlineCourseRepository.existsBySlug(COURSE_SLUG)) {
            return;
        }

        CourseCategory category = courseCategoryRepository.findByCode(CourseCategoryCode.IELTS.name())
                .orElseGet(() -> courseCategoryRepository.save(CourseCategory.builder()
                        .code(CourseCategoryCode.IELTS.name())
                        .name("IELTS")
                        .description("IELTS exam preparation courses")
                        .displayOrder(1)
                        .active(true)
                        .build()));

        OnlineCourse onlineCourse = onlineCourseRepository.findBySlug(COURSE_SLUG)
                .orElseGet(() -> OnlineCourse.builder()
                        .slug(COURSE_SLUG)
                        .build());

        onlineCourse.setTitle("E2 IELTS Practice");
        onlineCourse.setShortDescription("IELTS practice course curated from public E2 IELTS YouTube videos.");
        onlineCourse.setDescription("An IELTS practice course for Listening, Reading, and Speaking practice. Each video is organized as one module with a study guide, the original video lesson, and follow-up practice.");
        onlineCourse.setTargetScore("IELTS 5.5 - 7.0");
        onlineCourse.setDuration("5 hours 32 minutes");
        onlineCourse.setStudyMode("Self-paced online video course");
        onlineCourse.setPrice(BigDecimal.valueOf(1_190_000));
        onlineCourse.setThumbnailUrl("https://i.ytimg.com/vi/v3axTdVoYkY/hqdefault.jpg");
        onlineCourse.setStatus(PackageStatus.PUBLISHED);
        onlineCourse.setFeatured(true);
        onlineCourse.setDeleted(false);
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
        OnlineCourse savedOnlineCourse = onlineCourseRepository.save(onlineCourse);
        onlineCourse = savedOnlineCourse;

        addModule(onlineCourse, 1,
                "IELTS Listening Practice Test with Answers",
                "Practice a full Listening test and focus on main ideas, details, and answer checking.",
                "Listening",
                bunnyVideo("b0b3efc4-7fcf-46b9-840f-542b8c9bdd3e"),
                29,
                true,
                "Review the Listening test format and a quick question-reading strategy before you start.",
                "Log mistakes by type: keyword, synonym, number, spelling, and distractor.");

        addModule(onlineCourse, 2,
                "IELTS Reading Practice Test with Answer Explanations",
                "Reading practice with answer explanations for scanning, skimming, and locating evidence.",
                "Reading",
                bunnyVideo("6927ea2a-9592-4659-97df-97fdf542273a"),
                26,
                false,
                "Review how to identify keywords and predict where evidence appears in the passage.",
                "Create a short review sheet with keywords, paraphrases, and the reason each answer is correct.");

        addModule(onlineCourse, 3,
                "Full IELTS Listening Test with Answers | 2024",
                "A full Listening test to build pacing and time control under test-like conditions.",
                "Listening",
                bunnyVideo("eab36be2-3cca-4090-a187-8ee3cd0a1f60"),
                33,
                false,
                "Prepare an answer sheet and complete the test in one pass without pausing the video.",
                "Score your work, replay difficult segments, and write short transcripts for the hardest questions.");

        addModule(onlineCourse, 4,
                "IELTS Speaking Practice Test with Answers",
                "A simulated Speaking test to improve structure, examples, and natural delivery.",
                "Speaking",
                bunnyVideo("4f9949d9-0271-4298-996d-35bf79e9838e"),
                33,
                false,
                "Review Fluency, Lexical Resource, Grammar Range, and Pronunciation before watching.",
                "Record your own answers for three prompts and self-assess with the IELTS criteria.");

        addModule(onlineCourse, 5,
                "IELTS Listening: Techniques and Practice Questions",
                "Learn core Listening techniques and apply them in guided practice questions.",
                "Listening",
                bunnyVideo("05cda2ce-eae3-4dce-8e57-12f715cc311f"),
                36,
                false,
                "Focus on predicting, signposting, paraphrasing, and avoiding distractors.",
                "Collect ten useful keywords or paraphrases and turn them into a personal strategy note.");

        addModule(onlineCourse, 6,
                "100 IELTS Speaking Questions | Part 1 - 20+ IELTS Speaking Topics",
                "A speaking prompt bank across common Part 1 topics to build faster response habits.",
                "Speaking",
                bunnyVideo("e1bcc9de-991b-426d-8cc2-71baf0665ca9"),
                35,
                false,
                "Choose five familiar topics and outline short answers with the Answer-Explain-Example pattern.",
                "Build a personal speaking bank with twenty questions, idea prompts, and strong vocabulary.");

        backfillMissingVideoTranscripts(onlineCourse);
        onlineCourseVersionRepository.save(ensureDraftVersion(onlineCourse));
        onlineCourseRepository.save(onlineCourse);
        onlineCourseVersionService.refreshPublishedSnapshot(onlineCourse);
    }

    private void backfillMissingVideoTranscripts(OnlineCourse onlineCourse) {
        OnlineCourseVersion draftVersion = ensureDraftVersion(onlineCourse);
        draftVersion.getModules().stream()
                .flatMap(module -> module.getLessons().stream())
                .filter(lesson -> lesson.getVideoUrl() != null && !lesson.getVideoUrl().isBlank())
                .filter(lesson -> lesson.getVideoUrl().contains("youtube.com") || lesson.getVideoUrl().contains("youtu.be"))
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
            BunnyLessonVideo video,
            int videoDurationMinutes,
            boolean preview,
            String preLessonDescription,
            String postLessonDescription
    ) {
        OnlineCourseModule module = findModule(onlineCourse, order);
        module.setTitle("Module " + order + ": " + videoTitle);
        module.setDescription(moduleDescription);
        module.setSequenceNumber(order);

        upsertLesson(
                module,
                1,
                "OnlineLesson " + order + ".1: Goals and strategy for " + skill,
                preLessonDescription,
                buildTextLessonContent(order, 1, "Mục tiêu và chiến lược cho " + skill, preLessonDescription),
                null,
                10,
                preview
        );

        upsertLesson(
                module,
                2,
                "OnlineLesson " + order + ".2: Video practice - " + videoTitle,
                "Watch the original E2 IELTS video and track mistakes while following the guided practice flow.",
                buildVideoLessonContent(order, videoTitle),
                video,
                videoDurationMinutes,
                preview
        );

        upsertLesson(
                module,
                3,
                "OnlineLesson " + order + ".3: Review and post-video practice",
                postLessonDescription,
                buildTextLessonContent(order, 3, "Ôn tập sau video", postLessonDescription),
                null,
                15,
                false
        );

        module.getLessons().sort(Comparator.comparing(OnlineLesson::getSequenceNumber).thenComparing(lesson -> lesson.getId() == null ? Long.MAX_VALUE : lesson.getId()));
        module.getOnlineCourseVersion().getModules().sort(Comparator.comparing(OnlineCourseModule::getSequenceNumber).thenComparing(moduleItem -> moduleItem.getId() == null ? Long.MAX_VALUE : moduleItem.getId()));
    }

    private OnlineCourseModule findModule(OnlineCourse onlineCourse, int order) {
        OnlineCourseVersion draftVersion = ensureDraftVersion(onlineCourse);
        return draftVersion.getModules().stream()
                .filter(module -> module.getSequenceNumber() != null && module.getSequenceNumber() == order)
                .findFirst()
                .orElseGet(() -> {
                    OnlineCourseModule module = new OnlineCourseModule();
                    draftVersion.addModule(module);
                    return module;
                });
    }

    private OnlineCourseVersion ensureDraftVersion(OnlineCourse course) {
        return onlineCourseVersionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(course, CourseVersionStatus.DRAFT)
                .or(() -> onlineCourseVersionRepository.findFirstByOnlineCourseOrderByVersionNumberDesc(course))
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

    private void upsertLesson(OnlineCourseModule module, int order, String title, String description, String contentText, BunnyLessonVideo video, int durationMinutes, boolean preview) {
        OnlineLesson lesson = module.getLessons().stream()
                .filter(existingLesson -> existingLesson.getSequenceNumber() != null && existingLesson.getSequenceNumber() == order)
                .findFirst()
                .orElseGet(() -> {
                    OnlineLesson newLesson = new OnlineLesson();
                    module.addLesson(newLesson);
                    return newLesson;
                });

        lesson.setTitle(title);
        lesson.setDescription(description);
        lesson.setContentType(video == null ? "text" : "video");
        lesson.setContentText(contentText);
        lesson.setVideoUrl(video == null ? null : video.embedUrl());
        lesson.setBunnyVideoId(video == null ? null : video.videoId());
        lesson.setBunnyLibraryId(video == null ? null : video.libraryId());
        lesson.setBunnyCdnUrl(video == null ? null : video.embedUrl());
        lesson.setMaterialUrl(null);
        lesson.setDurationMinutes(durationMinutes);
        lesson.setSequenceNumber(order);
        lesson.setPreview(preview);
        int moduleOrder = module.getSequenceNumber() == null ? 0 : module.getSequenceNumber();
        lesson.setLessonKey("%s-m%d-l%d".formatted(COURSE_SLUG, moduleOrder, order));
    }

    private String buildTextLessonContent(int moduleOrder, int lessonOrder, String heading, String description) {
        return """
                ## OnlineLesson %d.%d: %s

                %s

                ### Việc cần làm
                - Đọc kỹ mục tiêu của bước học này trước khi bắt đầu.
                - Ghi lại 3-5 ý chính hoặc lỗi quan trọng cần lưu ý trong quá trình học.
                - Sau khi hoàn thành, đánh dấu bài học để mở bước tiếp theo của mô-đun.
                """.formatted(moduleOrder, lessonOrder, heading, description);
    }

    private String buildVideoLessonContent(int moduleOrder, String videoTitle) {
        return """
                ## OnlineLesson %d.2: Video practice - %s

                Xem video theo đúng tiến độ bài học và ghi lại lỗi hoặc chiến thuật làm bài hữu ích cho bản thân.

                ### Cách học với video
                - Xem video một lượt như bài thi thật, chỉ tạm dừng khi thật sự cần ghi chú.
                - Ghi lại câu sai, từ khóa bỏ lỡ, dấu hiệu nhiễu và mẹo xử lý được nhắc trong video.
                - Sau khi xem xong, tự tóm tắt 2-3 điều bạn cần luyện lại trước khi sang bước ôn tập.
                """.formatted(moduleOrder, videoTitle);
    }

    private BunnyLessonVideo bunnyVideo(String videoId) {
        String embedUrl = "https://iframe.mediadelivery.net/embed/%s/%s".formatted(BUNNY_LIBRARY_ID, videoId);
        return new BunnyLessonVideo(embedUrl, videoId, BUNNY_LIBRARY_ID);
    }

    private record BunnyLessonVideo(String embedUrl, String videoId, String libraryId) {
    }
}
