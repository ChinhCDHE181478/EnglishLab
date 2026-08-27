package fu.sep490.g23.backend.seed;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.course.CourseCategory;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.enums.CourseCategoryCode;
import fu.sep490.g23.backend.entity.course.enums.CourseLevel;
import fu.sep490.g23.backend.entity.course.enums.CourseVersionStatus;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.course.CourseCategoryRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseVersionRepository;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CenterSheetCourseCatalog {

    private final CourseCategoryRepository courseCategoryRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseVersionRepository onlineCourseVersionRepository;
    private final OnlineCourseVersionService onlineCourseVersionService;

    record CourseSpec(
            String slug,
            String title,
            String description,
            CourseCategoryCode category,
            CourseLevel level,
            double minBand,
            double targetBand,
            String pathCode,
            String pathName,
            int pathOrder,
            String nextSlug,
            int displayOrder,
            String thumbnailUrl,
            List<String> moduleTitles
    ) {}

    List<CourseSpec> specs() {
        return List.of(
                new CourseSpec(
                        "center-sheet-ielts-listening",
                        "IELTS Listening Foundations",
                        "Khóa luyện Listening theo 4 section, form completion, map labelling và lecture notes.",
                        CourseCategoryCode.IELTS, CourseLevel.INTERMEDIATE, 4.5, 6.5,
                        "IELTS_BAND_45_TO_65", "IELTS 4.5 to 6.5 Classroom Path", 1,
                        "center-sheet-ielts-reading", 10, "/course-covers/ielts-listening.png",
                        List.of("Section 1 Everyday Conversations", "Section 2 Public Talks", "Section 3 Academic Dialogue", "Section 4 Lectures")),
                new CourseSpec(
                        "center-sheet-ielts-reading",
                        "IELTS Academic Reading",
                        "Khóa Reading Academic: matching headings, True/False/Not Given, summary completion.",
                        CourseCategoryCode.IELTS, CourseLevel.INTERMEDIATE, 5.0, 6.5,
                        "IELTS_BAND_45_TO_65", "IELTS 4.5 to 6.5 Classroom Path", 2,
                        "center-sheet-ielts-writing", 11, "/course-covers/ielts-reading.png",
                        List.of("Skimming and Scanning", "True False Not Given", "Matching Headings", "Summary Completion")),
                new CourseSpec(
                        "center-sheet-ielts-writing",
                        "IELTS Writing Task 1 and Task 2",
                        "Khóa Writing: mô tả biểu đồ, luận điểm Task 2, paraphrase và cohesion.",
                        CourseCategoryCode.IELTS, CourseLevel.ADVANCED, 5.5, 7.0,
                        "IELTS_BAND_55_TO_70", "IELTS 5.5 to 7.0 Self-Paced Path", 3,
                        "center-sheet-ielts-speaking", 12, "/course-covers/ielts-writing.png",
                        List.of("Task 1 Charts", "Task 1 Processes", "Task 2 Opinion", "Task 2 Discussion")),
                new CourseSpec(
                        "center-sheet-ielts-speaking",
                        "IELTS Speaking Fluency Studio",
                        "Khóa Speaking Part 1-3, cue card, phát triển ý và collocation tự nhiên.",
                        CourseCategoryCode.IELTS, CourseLevel.INTERMEDIATE, 5.0, 6.5,
                        "IELTS_BAND_55_TO_70", "IELTS 5.5 to 7.0 Self-Paced Path", 4,
                        null, 13, "/course-covers/ielts-speaking.png",
                        List.of("Part 1 Daily Topics", "Part 2 Cue Cards", "Part 3 Abstract Ideas", "Pronunciation and Fluency")),
                new CourseSpec(
                        "center-sheet-toeic-lr",
                        "TOEIC Listening and Reading 650+",
                        "Khóa TOEIC L&R: photographs, Q&A, conversations, incomplete sentences và reading sets.",
                        CourseCategoryCode.TOEIC, CourseLevel.INTERMEDIATE, 4.0, 6.0,
                        "TOEIC_650_PATH", "TOEIC 650+ Workplace Path", 1,
                        "center-sheet-toeic-sw", 14, "/course-covers/toeic-lr.png",
                        List.of("Listening Photographs", "Listening Conversations", "Reading Incomplete Sentences", "Reading Passages")),
                new CourseSpec(
                        "center-sheet-toeic-sw",
                        "TOEIC Speaking and Writing 140+",
                        "Khóa TOEIC S&W: read aloud, describe picture, opinion essay và email writing.",
                        CourseCategoryCode.TOEIC, CourseLevel.INTERMEDIATE, 4.5, 6.0,
                        "TOEIC_650_PATH", "TOEIC 650+ Workplace Path", 2,
                        null, 15, "/course-covers/toeic-sw.png",
                        List.of("Read Aloud", "Describe a Picture", "Respond to Questions", "Opinion Essay")),
                new CourseSpec(
                        "center-sheet-communication-work",
                        "English Communication for Work",
                        "Khóa giao tiếp công sở: họp, email, thuyết trình và xử lý tình huống khách hàng.",
                        CourseCategoryCode.COMMUNICATION, CourseLevel.BEGINNER, 3.5, 5.5,
                        "COMMUNICATION_PATH", "Workplace Communication Path", 1,
                        "center-sheet-grammar-foundation", 16, "/course-covers/communication.png",
                        List.of("Meetings and Small Talk", "Emails and Requests", "Presentations", "Customer Situations")),
                new CourseSpec(
                        "center-sheet-grammar-foundation",
                        "English Grammar Foundation",
                        "Khóa ngữ pháp nền: thì, mệnh đề, giới từ và sửa lỗi thường gặp trong bài thi.",
                        CourseCategoryCode.FOUNDATION, CourseLevel.BEGINNER, 3.0, 5.0,
                        "COMMUNICATION_PATH", "Workplace Communication Path", 2,
                        "center-sheet-ielts-listening", 17, "/course-covers/grammar.png",
                        List.of("Tenses in Context", "Complex Sentences", "Prepositions and Articles", "Error Correction"))
        );
    }

    void seed(User contentManager) {
        for (CourseSpec spec : specs()) {
            upsertCourse(spec, contentManager);
        }
    }

    private void upsertCourse(CourseSpec spec, User contentManager) {
        CourseCategory category = courseCategoryRepository.findByCode(spec.category().name())
                .orElseGet(() -> courseCategoryRepository.save(CourseCategory.builder()
                        .code(spec.category().name())
                        .name(spec.category().name())
                        .description(spec.title())
                        .displayOrder(spec.displayOrder())
                        .active(true)
                        .build()));

        OnlineCourse course = onlineCourseRepository.findBySlug(spec.slug())
                .orElseGet(() -> OnlineCourse.builder()
                        .slug(spec.slug())
                        .build());
        course.setTitle(spec.title());
        course.setShortDescription(spec.description());
        course.setDescription(spec.description());
        course.setTargetScore(spec.category() == CourseCategoryCode.TOEIC ? "TOEIC 650+" : "IELTS " + spec.targetBand());
        course.setDuration("8 giờ học");
        course.setStudyMode("Tự học online, 4 module");
        course.setPrice(BigDecimal.valueOf(1_490_000));
        course.setThumbnailUrl(spec.thumbnailUrl());
        course.setStatus(PackageStatus.PUBLISHED);
        course.setDisplayOrder(spec.displayOrder());
        course.setFeatured(spec.pathOrder() <= 2);
        course.setDeleted(false);
        course.setCreatedBy(contentManager);
        course.setCategory(category);
        course.setLevel(spec.level());
        course.setRecommendedCurrentBandMin(spec.minBand());
        course.setTargetBand(spec.targetBand());
        course.setLearningPathCode(spec.pathCode());
        course.setLearningPathName(spec.pathName());
        course.setLearningPathOrder(spec.pathOrder());
        course.setRecommendedNextCourseSlug(spec.nextSlug());
        course.setTargetOutcome(spec.description());
        final OnlineCourse savedCourse = onlineCourseRepository.save(course);
        OnlineCourseVersion targetVersion = onlineCourseVersionRepository
                .findFirstByOnlineCourseAndStatusOrderByVersionNumberDesc(savedCourse, CourseVersionStatus.PUBLISHED)
                .or(() -> onlineCourseVersionRepository.findFirstByOnlineCourseOrderByVersionNumberDesc(savedCourse))
                .orElse(null);
        if (targetVersion == null || targetVersion.getModules() == null || targetVersion.getModules().isEmpty()) {
            targetVersion = ensureDraftVersion(savedCourse);
            int order = 1;
            for (String moduleTitle : spec.moduleTitles()) {
                OnlineCourseModule module = OnlineCourseModule.builder()
                        .title(moduleTitle)
                        .description("Module " + order + ": " + moduleTitle)
                        .sequenceNumber(order)
                        .build();
                module.addLesson(article(spec.slug(), order, moduleTitle + " - Orientation", 1, true,
                        "# " + moduleTitle + "\n\nMục tiêu: nắm chiến lược, từ vựng chủ đề và lỗi thường gặp.\n\n## Trước khi học\nViết 4 câu trả lời nhanh về chủ đề này, không dùng từ điển.\n\n## Cách học\n1. Đọc overview.\n2. Học collocation.\n3. Làm bài tập output."));
                module.addLesson(article(spec.slug(), order, moduleTitle + " - Strategy", 2, true,
                        "# Strategy\n\n- Đọc câu hỏi trước.\n- Gạch keyword.\n- Đoán loại thông tin (số, tên, danh từ).\n- Kiểm tra spelling.\n\nVí dụ: If the note says *opening time*, listen for a clock time such as 18:00."));
                module.addLesson(article(spec.slug(), order, moduleTitle + " - Vocabulary", 3, false,
                        "# Vocabulary bank\n\n1. **evening class** — lớp ca tối\n2. **intake** — đợt tuyển sinh\n3. **placement test** — bài xếp lớp\n4. **collocation** — cụm từ đi kèm\n5. **band descriptor** — mô tả band điểm\n\nViết 1 đoạn 80 từ dùng ít nhất 4 cụm trên."));
                module.addLesson(article(spec.slug(), order, moduleTitle + " - Practice", 4, false,
                        "# Practice\n\n1. Trả lời 3 câu Speaking trong 45 giây.\n2. Viết 1 đoạn Writing 120 từ.\n3. Ghi 5 lỗi bản thân hay mắc và cách sửa."));
                targetVersion.addModule(module);
                order++;
            }
            onlineCourseVersionRepository.save(targetVersion);
        }
        List<OnlineCourseModule> modulesForTotals = targetVersion.getModules() == null ? List.of() : targetVersion.getModules();
        int lessonCount = modulesForTotals.stream().mapToInt(module -> module.getLessons().size()).sum();
        savedCourse.setTotalLessons(lessonCount);
        savedCourse.setTotalHours(Math.max(4, lessonCount / 4));
        onlineCourseRepository.save(savedCourse);
        onlineCourseVersionService.refreshPublishedSnapshot(savedCourse);
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

    private OnlineLesson article(
            String courseSlug,
            int moduleOrder,
            String title,
            int lessonOrder,
            boolean preview,
            String content
    ) {
        return OnlineLesson.builder()
                .stableLessonKey("%s-m%d-l%d".formatted(courseSlug, moduleOrder, lessonOrder))
                .title(title)
                .description(title)
                .contentType("ARTICLE")
                .contentText(content)
                .durationMinutes(12)
                .sequenceNumber(lessonOrder)
                .preview(preview)
                .build();
    }
}
