package fu.sep490.g23.backend.service.course.impl;
import fu.sep490.g23.backend.entity.course.PackageEnrollment;
import fu.sep490.g23.backend.entity.course.CourseLessonFlashcardRef;
import fu.sep490.g23.backend.entity.course.CourseModule;
import fu.sep490.g23.backend.entity.course.VocabularyProgress;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.Lesson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.dto.response.course.VocabularyTermResponse;
import fu.sep490.g23.backend.dto.response.course.LessonResponse;
import fu.sep490.g23.backend.dto.response.course.ModuleResponse;
import fu.sep490.g23.backend.dto.response.course.OnlineCourseResponse;
import fu.sep490.g23.backend.dto.response.curriculum.FlashcardSetResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.commerce.CourseListItem;
import fu.sep490.g23.backend.entity.commerce.enums.CourseListType;
import fu.sep490.g23.backend.entity.course.enums.FlashcardPracticeSource;
import fu.sep490.g23.backend.entity.course.enums.VocabularyProgressStatus;
import fu.sep490.g23.backend.entity.course.*;
import fu.sep490.g23.backend.entity.curriculum.FlashcardSet;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.commerce.CourseListItemRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.PackageEnrollmentRepository;
import fu.sep490.g23.backend.repository.course.VocabularyProgressRepository;
import fu.sep490.g23.backend.service.course.FlashcardPracticeService;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FlashcardPracticeServiceImpl implements FlashcardPracticeService {
    private static final Pattern VOCABULARY_HEADING = Pattern.compile("(?m)^###\\s+\\d+\\.\\s+(.+)$");

    private final UserRepository userRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final PackageEnrollmentRepository enrollmentRepository;
    private final CourseListItemRepository courseListItemRepository;
    private final VocabularyProgressRepository progressRepository;
    private final OnlineCourseVersionService onlineCourseVersionService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public List<VocabularyTermResponse> getPracticeTerms(FlashcardPracticeSource source, Long courseId, boolean starredOnly, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail).orElseThrow(() -> new RuntimeException("Student not found"));
        FlashcardPracticeSource resolvedSource = source == null ? FlashcardPracticeSource.ENROLLED : source;
        if (resolvedSource == FlashcardPracticeSource.ENROLLED) {
            return getEnrolledVersionTerms(student, courseId, starredOnly);
        }
        List<OnlineCourse> courses = resolveCourses(resolvedSource, student).stream()
                .filter(course -> courseId == null || course.getId().equals(courseId))
                .toList();

        Map<String, VocabularyTermResponse> uniqueTerms = new LinkedHashMap<>();
        for (OnlineCourse course : courses) {
            initializeCourse(course);
            List<VocabularyProgress> progress = progressRepository.findByStudentAndCourse(student, course);
            for (VocabularyTermResponse term : extractTerms(course)) {
                applyProgress(term, progress);
                if (!starredOnly || term.isStarred()) {
                    uniqueTerms.putIfAbsent(term.getTermKey(), term);
                }
            }
        }
        return new ArrayList<>(uniqueTerms.values());
    }

    private List<VocabularyTermResponse> getEnrolledVersionTerms(User student, Long courseId, boolean starredOnly) {
        Map<String, VocabularyTermResponse> uniqueTerms = new LinkedHashMap<>();
        for (PackageEnrollment enrollment : enrollmentRepository.findByStudentOrderByRegisteredAtDesc(student)) {
            OnlineCourse course = onlineCourseRepository.findByLearningPackage(enrollment.getLearningPackage()).orElse(null);
            if (course == null || course.getLearningPackage().isDeleted()
                    || (courseId != null && !course.getId().equals(courseId))) {
                continue;
            }
            OnlineCourseResponse snapshot = onlineCourseVersionService.readLatestPublishedForEnrollment(enrollment, course);
            List<VocabularyProgress> progress = progressRepository.findByStudentAndCourse(student, course);
            for (VocabularyTermResponse term : extractTerms(snapshot)) {
                applyProgress(term, progress);
                if (!starredOnly || term.isStarred()) {
                    uniqueTerms.putIfAbsent(term.getTermKey(), term);
                }
            }
        }
        return new ArrayList<>(uniqueTerms.values());
    }

    private List<OnlineCourse> resolveCourses(FlashcardPracticeSource source, User student) {
        if (source == FlashcardPracticeSource.ENROLLED) {
            return enrollmentRepository.findByStudentOrderByRegisteredAtDesc(student).stream()
                    .map(enrollment -> onlineCourseRepository.findByLearningPackage(enrollment.getLearningPackage()).orElse(null))
                    .filter(course -> course != null && !course.getLearningPackage().isDeleted())
                    .toList();
        }
        if (source == FlashcardPracticeSource.WISHLIST) {
            return courseListItemRepository.findByStudentAndListTypeOrderByAddedAtDesc(student, CourseListType.WISHLIST).stream()
                    .map(CourseListItem::getOnlineCourse)
                    .filter(course -> course.getLearningPackage().isPublished())
                    .toList();
        }
        return onlineCourseRepository.findAll().stream()
                .filter(course -> course.getLearningPackage().isPublished())
                .toList();
    }

    private void initializeCourse(OnlineCourse course) {
        course.getModules().forEach(module -> module.getLessons().forEach(lesson ->
                lesson.getFlashcardRefs().forEach(ref -> ref.getFlashcardSet().getTitle())));
    }

    private List<VocabularyTermResponse> extractTerms(OnlineCourse course) {
        List<VocabularyTermResponse> bankTerms = new ArrayList<>();
        for (CourseModule module : course.getModules()) {
            for (Lesson lesson : module.getLessons()) {
                for (CourseLessonFlashcardRef ref : lesson.getFlashcardRefs()) {
                    if (!"ARCHIVED".equalsIgnoreCase(ref.getFlashcardSet().getStatus())) {
                        bankTerms.addAll(extractFlashcardSet(course, module, lesson, ref.getFlashcardSet()));
                    }
                }
            }
        }
        if (!bankTerms.isEmpty()) return bankTerms;

        List<VocabularyTermResponse> contentTerms = new ArrayList<>();
        for (CourseModule module : course.getModules()) {
            for (Lesson lesson : module.getLessons()) {
                contentTerms.addAll(extractLessonContent(course, module, lesson));
            }
        }
        return contentTerms;
    }

    private List<VocabularyTermResponse> extractTerms(OnlineCourseResponse course) {
        List<VocabularyTermResponse> bankTerms = new ArrayList<>();
        for (ModuleResponse module : safeModules(course)) {
            for (LessonResponse lesson : safeLessons(module)) {
                for (FlashcardSetResponse set : safeFlashcardSets(lesson)) {
                    if (!"ARCHIVED".equalsIgnoreCase(set.getStatus())) {
                        bankTerms.addAll(extractFlashcardSet(course, module, lesson, set));
                    }
                }
            }
        }
        if (!bankTerms.isEmpty()) {
            return bankTerms;
        }

        List<VocabularyTermResponse> contentTerms = new ArrayList<>();
        for (ModuleResponse module : safeModules(course)) {
            for (LessonResponse lesson : safeLessons(module)) {
                contentTerms.addAll(extractLessonContent(course, module, lesson));
            }
        }
        return contentTerms;
    }

    private List<VocabularyTermResponse> extractFlashcardSet(OnlineCourse course, CourseModule module, Lesson lesson, FlashcardSet set) {
        List<VocabularyTermResponse> terms = new ArrayList<>();
        try {
            JsonNode cards = objectMapper.readTree(set.getCardsJson() == null ? "[]" : set.getCardsJson());
            if (!cards.isArray()) return terms;
            int index = 0;
            for (JsonNode card : cards) {
                String term = firstText(card, "term", "front", "question", "word");
                String meaning = firstText(card, "meaning", "back", "answer", "definition");
                if (!term.isBlank() && !meaning.isBlank()) {
                    terms.add(baseTerm(course, module, lesson)
                            .termKey("flashcard-set-%s-%s-%s".formatted(set.getId(), index, slug(term)))
                            .term(term)
                            .meaning(meaning)
                            .example(firstText(card, "example", "sentence"))
                            .commonError(firstText(card, "commonError", "note"))
                            .build());
                }
                index += 1;
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return terms;
    }

    private List<VocabularyTermResponse> extractFlashcardSet(
            OnlineCourseResponse course,
            ModuleResponse module,
            LessonResponse lesson,
            FlashcardSetResponse set
    ) {
        List<VocabularyTermResponse> terms = new ArrayList<>();
        try {
            JsonNode cards = objectMapper.readTree(set.getCardsJson() == null ? "[]" : set.getCardsJson());
            if (!cards.isArray()) {
                return terms;
            }
            int index = 0;
            for (JsonNode card : cards) {
                String term = firstText(card, "term", "front", "question", "word");
                String meaning = firstText(card, "meaning", "back", "answer", "definition");
                if (!term.isBlank() && !meaning.isBlank()) {
                    terms.add(baseTerm(course, module, lesson)
                            .termKey("flashcard-set-%s-%s-%s".formatted(set.getId(), index, slug(term)))
                            .term(term)
                            .meaning(meaning)
                            .example(firstText(card, "example", "sentence"))
                            .commonError(firstText(card, "commonError", "note"))
                            .build());
                }
                index += 1;
            }
        } catch (Exception ignored) {
            return List.of();
        }
        return terms;
    }

    private List<VocabularyTermResponse> extractLessonContent(OnlineCourse course, CourseModule module, Lesson lesson) {
        String content = lesson.getContentText();
        if (content == null || !content.contains("### ")) return List.of();
        var headings = VOCABULARY_HEADING.matcher(content).results().toList();
        List<VocabularyTermResponse> terms = new ArrayList<>();
        for (int index = 0; index < headings.size(); index++) {
            var heading = headings.get(index);
            int end = index + 1 < headings.size() ? headings.get(index + 1).start() : content.length();
            String block = content.substring(heading.end(), end);
            String meaning = field(block, "Meaning");
            if (meaning.isBlank()) continue;
            String term = clean(heading.group(1));
            terms.add(baseTerm(course, module, lesson)
                    .termKey("%s-%s-%s".formatted(module.getId(), lesson.getId(), slug(term)))
                    .term(term)
                    .meaning(meaning)
                    .example(firstNonBlank(field(block, "IELTS example"), field(block, "Example")))
                    .commonError(firstNonBlank(field(block, "Common error to avoid"), field(block, "Common error")))
                    .build());
        }
        return terms;
    }

    private List<VocabularyTermResponse> extractLessonContent(
            OnlineCourseResponse course,
            ModuleResponse module,
            LessonResponse lesson
    ) {
        String content = lesson.getContentText();
        if (content == null || !content.contains("### ")) {
            return List.of();
        }
        var headings = VOCABULARY_HEADING.matcher(content).results().toList();
        List<VocabularyTermResponse> terms = new ArrayList<>();
        for (int index = 0; index < headings.size(); index++) {
            var heading = headings.get(index);
            int end = index + 1 < headings.size() ? headings.get(index + 1).start() : content.length();
            String block = content.substring(heading.end(), end);
            String meaning = field(block, "Meaning");
            if (meaning.isBlank()) {
                continue;
            }
            String term = clean(heading.group(1));
            terms.add(baseTerm(course, module, lesson)
                    .termKey("%s-%s-%s".formatted(module.getId(), lesson.getId(), slug(term)))
                    .term(term)
                    .meaning(meaning)
                    .example(firstNonBlank(field(block, "IELTS example"), field(block, "Example")))
                    .commonError(firstNonBlank(field(block, "Common error to avoid"), field(block, "Common error")))
                    .build());
        }
        return terms;
    }

    private VocabularyTermResponse.VocabularyTermResponseBuilder baseTerm(OnlineCourse course, CourseModule module, Lesson lesson) {
        return VocabularyTermResponse.builder()
                .courseId(course.getId())
                .courseTitle(course.getLearningPackage().getTitle())
                .moduleId(module.getId())
                .moduleTitle(module.getTitle())
                .lessonId(lesson.getId())
                .lessonTitle(lesson.getTitle())
                .status(VocabularyProgressStatus.NEW)
                .starred(false)
                .reviewCount(0)
                .correctCount(0)
                .incorrectCount(0);
    }

    private VocabularyTermResponse.VocabularyTermResponseBuilder baseTerm(
            OnlineCourseResponse course,
            ModuleResponse module,
            LessonResponse lesson
    ) {
        return VocabularyTermResponse.builder()
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .moduleId(module.getId())
                .moduleTitle(module.getTitle())
                .lessonId(lesson.getId())
                .lessonTitle(lesson.getTitle())
                .status(VocabularyProgressStatus.NEW)
                .starred(false)
                .reviewCount(0)
                .correctCount(0)
                .incorrectCount(0);
    }

    private List<ModuleResponse> safeModules(OnlineCourseResponse course) {
        return course.getModules() == null ? List.of() : course.getModules();
    }

    private List<LessonResponse> safeLessons(ModuleResponse module) {
        return module.getLessons() == null ? List.of() : module.getLessons();
    }

    private List<FlashcardSetResponse> safeFlashcardSets(LessonResponse lesson) {
        return lesson.getFlashcardSets() == null ? List.of() : lesson.getFlashcardSets();
    }

    private void applyProgress(VocabularyTermResponse term, List<VocabularyProgress> progressItems) {
        progressItems.stream().filter(item -> item.getTermKey().equals(term.getTermKey())).findFirst().ifPresent(progress -> {
            term.setStatus(progress.getStatus());
            term.setStarred(progress.isStarred());
            term.setReviewCount(progress.getReviewCount() == null ? 0 : progress.getReviewCount());
            term.setCorrectCount(progress.getCorrectCount() == null ? 0 : progress.getCorrectCount());
            term.setIncorrectCount(progress.getIncorrectCount() == null ? 0 : progress.getIncorrectCount());
            term.setLastResultCorrect(progress.getLastResultCorrect());
        });
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = clean(node.path(field).asText(""));
            if (!value.isBlank()) return value;
        }
        return "";
    }

    private String field(String block, String label) {
        var matcher = Pattern.compile("(?m)^\\*\\*" + Pattern.quote(label) + ":\\*\\*\\s*(.+)$", Pattern.CASE_INSENSITIVE).matcher(block);
        return matcher.find() ? clean(matcher.group(1)) : "";
    }

    private String clean(String value) {
        return value == null ? "" : value.replace("**", "").replaceAll("^[\"']|[\"']$", "").trim();
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String slug(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
    }
}
