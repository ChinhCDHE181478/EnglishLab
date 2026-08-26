package fu.sep490.g23.backend.seed;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.AssessmentSubmission;
import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.SubmissionStatus;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.LessonProgress;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseVersion;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.entity.course.enums.EnrollmentStatus;
import fu.sep490.g23.backend.entity.course.enums.LessonProgressStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.course.LessonProgressRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.ai.AiEvaluationResult;
import fu.sep490.g23.backend.service.course.CourseProgressService;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@Order(330)
@RequiredArgsConstructor
@Slf4j
public class ShowcaseLearnerCourseCompletionSeeder implements CommandLineRunner {
    private static final String LEARNER_EMAIL = "0386852628z@gmail.com";
    private static final String COURSE_SLUG = "e2-ielts-practice-tests";
    private static final String VOCABULARY_COURSE_SLUG = "ielts-master-vocabulary-band-7-plus";
    private static final int VOCABULARY_COMPLETED_MODULE_COUNT = 2;
    private static final String DEMO_AUDIO_FILE = "assessment-audio-e2-showcase.wav";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final UserRepository userRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseEnrollmentRepository enrollmentRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final CourseAssessmentRepository assessmentRepository;
    private final AssessmentSubmissionRepository submissionRepository;
    private final OnlineCourseVersionService courseVersionService;
    private final CourseProgressService courseProgressService;
    private final AiEvaluationClient aiEvaluationClient;

    @Value("${app.seed.sheet.enabled:false}")
    private boolean seedEnabled;

    @Value("${englishlab.assessment-audio.dir:backend/uploads/assessment-audio}")
    private String assessmentAudioDirectory;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            return;
        }

        User learner = userRepository.findByEmail(LEARNER_EMAIL).orElse(null);
        if (learner == null) {
            log.warn("Không thể hoàn thiện khóa demo vì thiếu học viên {}.", LEARNER_EMAIL);
            return;
        }
        completePublishedCourse(learner, COURSE_SLUG, 7);
        seedVocabularyCourseLessonProgress(learner);
    }

    private void completePublishedCourse(User learner, String slug, int minAssessments) {
        OnlineCourse course = onlineCourseRepository.findBySlug(slug).orElse(null);
        if (course == null) {
            log.warn("Không thể hoàn thiện khóa {} cho tài khoản demo vì thiếu dữ liệu OnlineCourse.", slug);
            return;
        }

        List<CourseAssessment> assessments = assessmentRepository
                .findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course);
        if (assessments.size() < minAssessments) {
            log.warn("Không thể hoàn thiện khóa {} cho tài khoản demo vì mới có {}/{} bài đánh giá.",
                    slug, assessments.size(), minAssessments);
            return;
        }

        courseVersionService.refreshPublishedSnapshot(course);
        OnlineCourseVersion version = courseVersionService.requirePublishedVersion(course);
        OnlineCourseEnrollment enrollment = enrollmentRepository.findByStudentAndOnlineCourse(learner, course)
                .orElseGet(() -> enrollmentRepository.save(OnlineCourseEnrollment.builder()
                        .student(learner)
                        .onlineCourse(course)
                        .registeredAt(LocalDateTime.now().minusDays(45))
                        .build()));
        enrollment.setCourseVersion(version);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment = enrollmentRepository.save(enrollment);

        completeLessons(course, learner, enrollment, version);
        seedAssessmentSubmissions(assessments, learner);
        backfillCorrectedExamples(assessments, learner);
        OnlineCourseEnrollment completed = courseProgressService.refreshEnrollmentProgress(enrollment, course, learner);
        if (completed.getProgressPercent() != 100) {
            log.warn("Khóa {} của tài khoản demo chưa đủ điều kiện hoàn thành: {}% - {}.",
                    slug, completed.getProgressPercent(), completed.getStatus());
            return;
        }
        log.info("Đã hoàn thiện khóa {} cho {}.", course.getTitle(), LEARNER_EMAIL);
    }

    private void seedVocabularyCourseLessonProgress(User learner) {
        OnlineCourse course = onlineCourseRepository.findBySlug(VOCABULARY_COURSE_SLUG).orElse(null);
        if (course == null) {
            log.warn("Không thể tạo tiến độ vocabulary demo vì thiếu OnlineCourse.");
            return;
        }

        List<CourseAssessment> assessments = assessmentRepository
                .findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course);
        courseVersionService.refreshPublishedSnapshot(course);
        OnlineCourseVersion version = courseVersionService.requirePublishedVersion(course);
        OnlineCourseEnrollment enrollment = enrollmentRepository.findByStudentAndOnlineCourse(learner, course)
                .orElseGet(() -> enrollmentRepository.save(OnlineCourseEnrollment.builder()
                        .student(learner)
                        .onlineCourse(course)
                        .registeredAt(LocalDateTime.now().minusDays(40))
                        .build()));
        enrollment.setCourseVersion(version);
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        enrollment = enrollmentRepository.save(enrollment);

        completeLessons(course, learner, enrollment, version, VOCABULARY_COMPLETED_MODULE_COUNT);
        pruneLessonProgressBeyondModules(course, enrollment, VOCABULARY_COMPLETED_MODULE_COUNT);
        if (!assessments.isEmpty()) {
            List<AssessmentSubmission> leftover = submissionRepository.findByAssessmentInAndStudent(assessments, learner);
            if (!leftover.isEmpty()) {
                submissionRepository.deleteAll(leftover);
                submissionRepository.flush();
            }
        }

        List<CourseAssessment> earlyModuleTests = assessments.stream()
                .filter(assessment -> assessment.getDisplayOrder() != null && assessment.getDisplayOrder() <= 2)
                .sorted(Comparator.comparing(CourseAssessment::getDisplayOrder))
                .toList();
        for (CourseAssessment assessment : earlyModuleTests) {
            VocabularyDemoAttempt attempt = vocabularyAttempt(assessment);
            if (attempt == null) {
                continue;
            }
            submissionRepository.save(baseSubmission(assessment, learner)
                    .submittedText(attempt.answer())
                    .aiScore(attempt.score())
                    .aiFeedbackJson(attempt.feedbackJson())
                    .aiPromptSnapshot("Đánh giá bài output vocabulary theo rubric nghĩa, collocation, câu và mức bám chủ đề.")
                    .aiProvider("EnglishLab")
                    .aiModel("vocabulary-review-seed")
                    .aiRawResponse("Deterministic vocabulary review aligned with the submitted sentences.")
                    .status(SubmissionStatus.PASSED)
                    .build());
        }

        OnlineCourseEnrollment refreshed = courseProgressService.refreshEnrollmentProgress(enrollment, course, learner);
        log.info("Khóa vocabulary demo của {}: {}% - {} (2 mô-đun đầu đã xong; module test 3-8 chưa nộp).",
                LEARNER_EMAIL, refreshed.getProgressPercent(), refreshed.getStatus());
    }

    private VocabularyDemoAttempt vocabularyAttempt(CourseAssessment assessment) {
        int order = assessment.getDisplayOrder() == null ? 0 : assessment.getDisplayOrder();
        if (order == 1) {
            String answer = """
                    Many university students still rely on their immediate family when they first live away from home.
                    A supportive upbringing can nurture confidence, but it should not remove every difficult decision.
                    In my city, grandparents in the extended family often help with childcare, and that quietly changes family dynamics.
                    People who work abroad try to maintain close ties through evening video calls rather than rare holiday visits.
                    An intergenerational gap can appear when parents and children disagree about marriage or career choices.
                    Siblings also teach young people how to negotiate, especially in crowded households.
                    """;
            return new VocabularyDemoAttempt(answer, BigDecimal.valueOf(7.0), vocabularyFeedback(
                    BigDecimal.valueOf(7.0),
                    "Bài làm dùng đúng từ vựng chủ đề Family and Relationships trong câu kiểu IELTS, không lạc sang tự đánh giá quá trình học.",
                    List.of(
                            criterion("Meaning Accuracy", 7.0, "immediate family, upbringing, nurture, extended family được dùng đúng nghĩa trong ngữ cảnh gia đình."),
                            criterion("Collocation", 7.0, "maintain close ties, family dynamics, intergenerational gap là các cụm tự nhiên, đúng ngữ vực Writing/Speaking."),
                            criterion("Sentence Quality", 6.5, "Câu phức rõ ý; còn một chỗ hơi dài và có thể tách để nhịp câu ổn định hơn."),
                            criterion("Topic Relevance", 7.5, "Nội dung bám đúng yêu cầu: 6 câu IELTS về gia đình, có ít nhất năm target words.")
                    ),
                    "Dùng được immediate family, upbringing, nurture, extended family, maintain close ties và family dynamics trong câu có ví dụ cụ thể.",
                    "Câu về grandparents hơi dài; có thể tách thành hai câu để phần family dynamics nổi hơn.",
                    "The grandparents in the extended family often help with childcare, and that quietly changes family dynamics.",
                    "Grandparents in the extended family often help with childcare, and this quietly changes family dynamics.",
                    "Bỏ the trước grandparents khi nói chung, và dùng this để chỉ cả hành động giúp trông trẻ."
            ));
        }
        if (order == 2) {
            String answer = """
                    Many economies still depend heavily on fossil fuels, so carbon emissions remain difficult to cut quickly.
                    Some warming may be inevitable, yet several cities have made major strides in public transport.
                    Investment in renewable energy is more realistic than asking every household to change overnight.
                    Governments can stem the rate of increase in private car use through congestion charges.
                    Unchecked construction also accelerates environmental degradation around river basins.
                    Long-term sustainability therefore needs industrial reform as well as daily consumption choices.
                    """;
            return new VocabularyDemoAttempt(answer, BigDecimal.valueOf(7.0), vocabularyFeedback(
                    BigDecimal.valueOf(7.0),
                    "Bài làm viết 6 câu IELTS về Climate Change và dùng đủ nhóm từ mục tiêu: fossil fuels, carbon emissions, renewable energy, environmental degradation.",
                    List.of(
                            criterion("Meaning Accuracy", 7.0, "fossil fuels, carbon emissions, inevitable, renewable energy được dùng đúng nghĩa, không lẫn với recycled energy hay pollution chung chung."),
                            criterion("Collocation", 7.5, "depend heavily on fossil fuels, made major strides in, stem the rate of increase, environmental degradation đều là collocation học thuật."),
                            criterion("Sentence Quality", 6.5, "Cú pháp ổn định; therefore ở câu cuối hơi nặng, nên giữ một liên từ kết luận là đủ."),
                            criterion("Topic Relevance", 7.5, "Đúng đề output: nguyên nhân, giải pháp và từ vựng Climate Change, không phải nhật ký học tập.")
                    ),
                    "Bài có nguyên nhân (fossil fuels), hệ quả (carbon emissions, environmental degradation) và hướng xử lý (renewable energy, congestion charges).",
                    "Câu kết dùng therefore hơi trang trọng so với các câu trước; có thể nối trực tiếp bằng so hoặc This means.",
                    "Long-term sustainability therefore needs industrial reform as well as daily consumption choices.",
                    "Long-term sustainability needs industrial reform as well as daily consumption choices.",
                    "Bỏ therefore vì ý kết luận đã đủ rõ sau chuỗi giải pháp."
            ));
        }
        return null;
    }

    private String vocabularyFeedback(
            BigDecimal score,
            String summary,
            List<ObjectNode> criteria,
            String strength,
            String weakness,
            String original,
            String corrected,
            String explanation
    ) {
        ObjectNode feedback = OBJECT_MAPPER.createObjectNode();
        feedback.put("estimatedScore", score);
        feedback.put("estimatedBand", "7.0");
        feedback.put("skill", AssessmentSkill.VOCABULARY.name());
        feedback.put("summary", summary);
        ArrayNode criteriaNode = OBJECT_MAPPER.createArrayNode();
        criteria.forEach(criteriaNode::add);
        feedback.set("criteria", criteriaNode);
        feedback.set("strengths", OBJECT_MAPPER.createArrayNode().add(strength));
        feedback.set("weaknesses", OBJECT_MAPPER.createArrayNode().add(weakness));
        feedback.set("suggestions", OBJECT_MAPPER.createArrayNode()
                .add("Giữ thói quen viết 5-7 câu có target words trước, rồi mới rút gọn câu dài."));
        feedback.set("recommendedReview", OBJECT_MAPPER.createArrayNode()
                .add("Ôn lại collocation trong vocabulary bank rồi viết thêm một đoạn Task 2 cùng chủ đề."));
        feedback.set("correctedExamples", OBJECT_MAPPER.createArrayNode()
                .add(correctedExample(original, corrected, explanation)));
        ArrayNode partFeedback = OBJECT_MAPPER.createArrayNode();
        ObjectNode part = OBJECT_MAPPER.createObjectNode();
        part.put("partKey", "part_1");
        part.put("partLabel", "Câu IELTS dùng từ vựng mục tiêu");
        part.put("summary", summary);
        part.set("strengths", OBJECT_MAPPER.createArrayNode().add(strength));
        part.set("weaknesses", OBJECT_MAPPER.createArrayNode().add(weakness));
        part.set("suggestions", OBJECT_MAPPER.createArrayNode()
                .add("Đọc lại đề: cần câu tiếng Anh theo chủ đề module, không phải nhận xét về cách học."));
        partFeedback.add(part);
        feedback.set("partFeedback", partFeedback);
        feedback.put("plagiarismRisk", "LOW");
        feedback.put("aiUsageRisk", "LOW");
        feedback.set("sourceSignals", OBJECT_MAPPER.createArrayNode()
                .add("Các câu có ví dụ đời sống và collocation khớp vocabulary bank của module."));
        ObjectNode originality = OBJECT_MAPPER.createObjectNode();
        originality.put("summary", "Bài làm dùng từ mục tiêu trong câu mới, không chép nguyên model paragraph của bài học.");
        originality.put("plagiarismRisk", "LOW");
        originality.put("aiUsageRisk", "LOW");
        feedback.set("originalityAnalysis", originality);
        feedback.put("disclaimer", "Đây là phản hồi hỗ trợ học tập, không phải điểm IELTS chính thức.");
        return feedback.toString();
    }

    private ObjectNode criterion(String name, double score, String feedback) {
        ObjectNode criterion = OBJECT_MAPPER.createObjectNode();
        criterion.put("name", name);
        criterion.put("score", score);
        criterion.put("feedback", feedback);
        return criterion;
    }

    private record VocabularyDemoAttempt(String answer, BigDecimal score, String feedbackJson) {
    }

    private void completeLessons(
            OnlineCourse course,
            User learner,
            OnlineCourseEnrollment enrollment,
            OnlineCourseVersion version
    ) {
        completeLessons(course, learner, enrollment, version, Integer.MAX_VALUE);
    }

    private void completeLessons(
            OnlineCourse course,
            User learner,
            OnlineCourseEnrollment enrollment,
            OnlineCourseVersion version,
            int maxModules
    ) {
        List<OnlineLesson> lessons = orderedModules(course).stream()
                .limit(maxModules)
                .flatMap(module -> module.getLessons().stream()
                        .sorted(Comparator.comparing(lesson -> lesson.getDisplayOrder() == null ? Integer.MAX_VALUE : lesson.getDisplayOrder())))
                .toList();
        LocalDateTime firstCompletion = LocalDateTime.now().minusDays(30);
        for (int index = 0; index < lessons.size(); index++) {
            OnlineLesson lesson = lessons.get(index);
            LessonProgress progress = lessonProgressRepository.findByStudentAndLesson(learner, lesson)
                    .orElseGet(() -> LessonProgress.builder()
                            .student(learner)
                            .lesson(lesson)
                            .enrollment(enrollment)
                            .build());
            LocalDateTime completedAt = firstCompletion.plusDays(index);
            progress.setEnrollment(enrollment);
            progress.setCourseVersion(version);
            progress.setLessonKey(lesson.getLessonKey());
            progress.setStatus(LessonProgressStatus.COMPLETED);
            progress.setProgressPercent(100);
            progress.setCompletedAt(completedAt);
            progress.setLastAccessedAt(completedAt.plusMinutes(45));
            lessonProgressRepository.save(progress);
        }
    }

    private void pruneLessonProgressBeyondModules(
            OnlineCourse course,
            OnlineCourseEnrollment enrollment,
            int keepModuleCount
    ) {
        Set<Long> allowedLessonIds = orderedModules(course).stream()
                .limit(keepModuleCount)
                .flatMap(module -> module.getLessons().stream())
                .map(OnlineLesson::getId)
                .collect(Collectors.toSet());
        List<LessonProgress> extra = lessonProgressRepository.findByEnrollment(enrollment).stream()
                .filter(progress -> progress.getLesson() == null || !allowedLessonIds.contains(progress.getLesson().getId()))
                .toList();
        if (!extra.isEmpty()) {
            lessonProgressRepository.deleteAll(extra);
            lessonProgressRepository.flush();
        }
    }

    private List<OnlineCourseModule> orderedModules(OnlineCourse course) {
        return course.getModules().stream()
                .sorted(Comparator.comparing(module -> module.getDisplayOrder() == null ? Integer.MAX_VALUE : module.getDisplayOrder()))
                .toList();
    }

    private void seedAssessmentSubmissions(List<CourseAssessment> assessments, User learner) {
        byte[] speakingAudio = loadDemoSpeakingAudio();
        String speakingAudioUrl = speakingAudio == null ? null : stageDemoSpeakingAudio(speakingAudio);

        for (CourseAssessment assessment : assessments) {
            AssessmentSubmission existing = submissionRepository
                    .findTopByAssessmentAndStudentOrderBySubmittedAtDesc(assessment, learner)
                    .orElse(null);
            if (existing != null) {
                BigDecimal score = normalizeDemoScore(existing.getAiScore(), assessment);
                existing.setAiScore(score);
                existing.setStatus(resolveSubmissionStatus(score, assessment));
                existing.setAiFeedbackJson(ensureFeedbackScore(existing.getAiFeedbackJson(), score, assessment.getSkill()));
                submissionRepository.save(existing);
                continue;
            }
            AssessmentSubmission submission = switch (assessment.getSkill()) {
                case LISTENING, READING -> buildObjectiveSubmission(assessment, learner);
                case WRITING -> buildAiSubmission(assessment, learner, writingAnswer(), null, null);
                case SPEAKING -> buildAiSubmission(
                        assessment,
                        learner,
                        speakingAnswer(assessment),
                        speakingAudioUrl,
                        speakingAudio
                );
                default -> buildAiSubmission(assessment, learner, reflectionAnswer(), null, null);
            };
            submissionRepository.save(submission);
        }
    }

    private AssessmentSubmission buildObjectiveSubmission(CourseAssessment assessment, User learner) {
        try {
            JsonNode answerKey = OBJECT_MAPPER.readTree(assessment.getObjectiveAnswerKey());
            ArrayNode responses = OBJECT_MAPPER.createArrayNode();
            answerKey.fields().forEachRemaining(entry -> {
                ObjectNode response = OBJECT_MAPPER.createObjectNode();
                int questionNumber = Integer.parseInt(entry.getKey());
                response.put("questionNumber", entry.getKey());
                response.put("part", objectivePart(assessment.getSkill(), questionNumber));
                if (entry.getValue().isArray()) {
                    response.put("answerType", "multi_select_letters");
                    response.put("answer", String.join(",", OBJECT_MAPPER.convertValue(entry.getValue(), String[].class)));
                } else {
                    response.put("answerType", "text");
                    response.put("answer", entry.getValue().asText());
                }
                responses.add(response);
            });
            ObjectNode objectiveAnswers = OBJECT_MAPPER.createObjectNode();
            objectiveAnswers.set("responses", responses);
            int total = answerKey.size();
            String feedback = objectiveFeedback(assessment.getSkill(), total);
            return baseSubmission(assessment, learner)
                    .objectiveAnswersJson(OBJECT_MAPPER.writeValueAsString(objectiveAnswers))
                    .aiScore(BigDecimal.valueOf(total))
                    .aiFeedbackJson(feedback)
                    .aiPromptSnapshot("Đối chiếu từng câu trả lời với đáp án chuẩn của đề đã xuất bản.")
                    .aiProvider("EnglishLab")
                    .aiModel("objective-answer-key")
                    .aiRawResponse("Deterministic objective scoring from stored answer key")
                    .status(SubmissionStatus.PASSED)
                    .build();
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo bài làm khách quan cho tài khoản demo.", exception);
        }
    }

    private AssessmentSubmission buildAiSubmission(
            CourseAssessment assessment,
            User learner,
            String answer,
            String audioUrl,
            byte[] audioBytes
    ) {
        String prompt = buildEvaluationPrompt(assessment, answer, audioBytes != null);
        AiEvaluationResult result;
        try {
            result = audioBytes == null
                    ? aiEvaluationClient.evaluate(prompt)
                    : aiEvaluationClient.evaluateWithAudio(prompt, audioBytes, "audio/wav");
        } catch (RuntimeException exception) {
            log.warn("AI không khả dụng khi tạo bài demo '{}': {}. Dùng bản đánh giá dự phòng có đánh dấu rõ nguồn.",
                    assessment.getTitle(), exception.getMessage());
            result = fallbackEvaluation(assessment);
        }

        BigDecimal score = normalizeDemoScore(result.getEstimatedScore(), assessment);
        SubmissionStatus status = resolveSubmissionStatus(score, assessment);
        return baseSubmission(assessment, learner)
                .submittedText(answer)
                .submittedAudioUrl(audioUrl)
                .microphoneChecked(audioBytes == null ? null : true)
                .deviceCheckPassed(audioBytes == null ? null : true)
                .fullscreenExitCount(0)
                .tabSwitchCount(0)
                .aiScore(score)
                .aiFeedbackJson(ensureFeedbackScore(result.getFeedbackJson(), score, assessment.getSkill()))
                .aiPromptSnapshot(prompt)
                .aiProvider(result.getProvider())
                .aiModel(result.getModel())
                .aiRawResponse(result.getRawResponse())
                .status(status)
                .build();
    }

    private AssessmentSubmission.AssessmentSubmissionBuilder baseSubmission(CourseAssessment assessment, User learner) {
        return AssessmentSubmission.builder()
                .assessment(assessment)
                .student(learner);
    }

    private SubmissionStatus resolveSubmissionStatus(BigDecimal score, CourseAssessment assessment) {
        if (assessment.getAiEvaluationMode() == AiEvaluationMode.EXPLAIN_ONLY || score == null) {
            return SubmissionStatus.AI_EVALUATED;
        }
        BigDecimal passingScore = assessment.getPassingScore();
        return passingScore == null || score.compareTo(passingScore) >= 0
                ? SubmissionStatus.PASSED
                : SubmissionStatus.NEEDS_IMPROVEMENT;
    }

    private BigDecimal normalizeDemoScore(BigDecimal score, CourseAssessment assessment) {
        if (assessment.getAiEvaluationMode() == AiEvaluationMode.EXPLAIN_ONLY) {
            return null;
        }
        if (score == null) {
            score = BigDecimal.valueOf(6.5);
        }
        BigDecimal maxScore = assessment.getMaxScore() == null ? BigDecimal.valueOf(9) : assessment.getMaxScore();
        BigDecimal passingScore = assessment.getPassingScore();
        if (passingScore != null && score.compareTo(passingScore) < 0) {
            score = passingScore;
        }
        return score.max(BigDecimal.ZERO).min(maxScore);
    }

    private String buildEvaluationPrompt(CourseAssessment assessment, String answer, boolean hasAudio) {
        String criteria = assessment.getRubric() == null ? "Không có rubric định lượng; hãy phân tích lỗi và kế hoạch ôn tập."
                : assessment.getRubric().getCriteria().stream()
                .sorted(Comparator.comparing(criterion -> criterion.getDisplayOrder() == null ? Integer.MAX_VALUE : criterion.getDisplayOrder()))
                .map(criterion -> "- " + criterion.getName() + ": " + criterion.getDescription())
                .reduce("", (left, right) -> left + right + "\n");
        return """
                Bạn là AI chấm bài của EnglishLab. Hãy đánh giá bài làm demo dưới đây như một bài làm thật.
                Trả về JSON hợp lệ, không dùng markdown, với các trường: estimatedScore, estimatedBand,
                summary, criteria (name, score, feedback), strengths, weaknesses, suggestions,
                recommendedReview, correctedExamples, partFeedback, plagiarismRisk, aiUsageRisk,
                sourceSignals, originalityAnalysis và disclaimer.
                Nhận xét chủ yếu bằng tiếng Việt, cụ thể theo bằng chứng trong bài. Điểm IELTS chỉ dùng nấc 0.5.
                Đây không phải điểm IELTS chính thức.

                Kỹ năng: %s
                Chế độ chấm: %s
                Điểm tối đa: %s
                Có âm thanh thật để phân tích: %s
                Đề bài: %s
                Rubric:
                %s
                Bài làm:
                %s
                """.formatted(
                assessment.getSkill(),
                assessment.getAiEvaluationMode(),
                assessment.getMaxScore(),
                hasAudio,
                assessment.getInstructions(),
                criteria,
                answer
        );
    }

    private AiEvaluationResult fallbackEvaluation(CourseAssessment assessment) {
        BigDecimal score = assessment.getAiEvaluationMode() == AiEvaluationMode.EXPLAIN_ONLY
                ? null
                : BigDecimal.valueOf(6.5);
        ObjectNode feedback = detailedFeedback(
                score,
                assessment.getSkill(),
                "Bài làm đáp ứng đúng yêu cầu và thể hiện khả năng triển khai ý tương đối rõ ràng.",
                "Cần tăng độ chính xác ở một số cấu trúc phức và mở rộng dẫn chứng cụ thể hơn."
        );
        return AiEvaluationResult.builder()
                .estimatedScore(score)
                .feedbackJson(feedback.toString())
                .provider("EnglishLab")
                .model("review-seed-fallback")
                .rawResponse("AI provider unavailable; deterministic review seed fallback was used.")
                .audioInputAnalyzed(false)
                .build();
    }

    private String ensureFeedbackScore(String feedbackJson, BigDecimal score, AssessmentSkill assessmentSkill) {
        try {
            ObjectNode feedback = feedbackJson == null || feedbackJson.isBlank()
                    ? OBJECT_MAPPER.createObjectNode()
                    : (ObjectNode) OBJECT_MAPPER.readTree(feedbackJson);
            if (score == null) {
                feedback.putNull("estimatedScore");
            } else {
                feedback.put("estimatedScore", score);
            }
            if (!feedback.has("disclaimer")) {
                feedback.put("disclaimer", "Đây là phản hồi hỗ trợ học tập, không phải điểm IELTS chính thức.");
            }
            AssessmentSkill skill = assessmentSkill == null ? AssessmentSkill.MIXED : assessmentSkill;
            ensureCorrectedExamples(feedback, skill);
            return OBJECT_MAPPER.writeValueAsString(feedback);
        } catch (Exception exception) {
            return detailedFeedback(
                    score,
                    AssessmentSkill.MIXED,
                    "Bài làm đã được chấm và lưu kết quả.",
                    "Cần xem lại nhận xét chi tiết trước khi thực hiện lượt luyện tiếp theo."
            ).toString();
        }
    }

    private String objectiveFeedback(AssessmentSkill skill, int total) {
        ObjectNode feedback = detailedFeedback(
                BigDecimal.valueOf(total),
                skill,
                "Hoàn thành chính xác toàn bộ " + total + " đáp án của bài thi.",
                "Không có câu sai; nên duy trì quy trình kiểm tra spelling và evidence trước khi nộp."
        );
        feedback.put("rawScore", total);
        feedback.put("totalQuestions", total);
        feedback.put("correctCount", total);
        feedback.put("incorrectCount", 0);
        feedback.put("estimatedBand", "9.0");
        return feedback.toString();
    }

    private ObjectNode detailedFeedback(BigDecimal score, AssessmentSkill skill, String summary, String weakness) {
        ObjectNode feedback = OBJECT_MAPPER.createObjectNode();
        if (score == null) {
            feedback.putNull("estimatedScore");
            feedback.put("estimatedBand", "");
        } else {
            feedback.put("estimatedScore", score);
            feedback.put("estimatedBand", score.toPlainString());
        }
        feedback.put("skill", skill.name());
        feedback.put("summary", summary);
        feedback.set("criteria", OBJECT_MAPPER.createArrayNode());
        feedback.set("strengths", OBJECT_MAPPER.createArrayNode().add("Bài làm bám sát yêu cầu và có cấu trúc rõ ràng."));
        feedback.set("weaknesses", OBJECT_MAPPER.createArrayNode().add(weakness));
        feedback.set("suggestions", OBJECT_MAPPER.createArrayNode()
                .add("Xem lại các lỗi đã chỉ ra và thực hiện thêm một lượt luyện có bấm giờ."));
        feedback.set("recommendedReview", OBJECT_MAPPER.createArrayNode()
                .add("Ôn lại chiến lược kiểm tra đáp án và tiêu chí của kỹ năng này."));
        ensureCorrectedExamples(feedback, skill);
        feedback.set("partFeedback", OBJECT_MAPPER.createArrayNode());
        feedback.put("plagiarismRisk", "LOW");
        feedback.put("aiUsageRisk", "LOW");
        feedback.set("sourceSignals", OBJECT_MAPPER.createArrayNode()
                .add("Bài làm có nội dung liên tục, ví dụ cụ thể và cấu trúc phù hợp với yêu cầu."));
        ObjectNode originality = OBJECT_MAPPER.createObjectNode();
        originality.put("summary", "Không phát hiện dấu hiệu sao chép rõ ràng trong dữ liệu bài làm.");
        originality.put("plagiarismRisk", "LOW");
        originality.put("aiUsageRisk", "LOW");
        feedback.set("originalityAnalysis", originality);
        feedback.put("disclaimer", "Đây là phản hồi hỗ trợ học tập, không phải điểm IELTS chính thức.");
        return feedback;
    }

    private void backfillCorrectedExamples(List<CourseAssessment> assessments, User learner) {
        if (assessments == null || assessments.isEmpty()) {
            return;
        }
        for (AssessmentSubmission submission : submissionRepository.findByAssessmentInAndStudent(assessments, learner)) {
            submission.setAiFeedbackJson(ensureFeedbackScore(
                    submission.getAiFeedbackJson(),
                    submission.getAiScore(),
                    submission.getAssessment().getSkill()
            ));
            submissionRepository.save(submission);
        }
    }

    private void ensureCorrectedExamples(ObjectNode feedback, AssessmentSkill skill) {
        ArrayNode cleaned = OBJECT_MAPPER.createArrayNode();
        JsonNode existing = feedback.get("correctedExamples");
        if (existing != null && existing.isArray()) {
            for (JsonNode item : existing) {
                ObjectNode filled = fillExampleNode(item);
                if (filled != null) {
                    cleaned.add(filled);
                }
            }
        }
        if (cleaned.isEmpty() || examplesMismatchSkill(cleaned, skill)) {
            cleaned.removeAll();
            defaultCorrectedExamples(skill).forEach(cleaned::add);
        }
        feedback.set("correctedExamples", cleaned);
    }

    private boolean examplesMismatchSkill(ArrayNode examples, AssessmentSkill skill) {
        if (skill == AssessmentSkill.WRITING || skill == AssessmentSkill.MIXED) {
            return false;
        }
        String joined = examples.toString().toLowerCase(Locale.ROOT);
        return joined.contains("diagram") || joined.contains("milled corn");
    }

    private ObjectNode fillExampleNode(JsonNode item) {
        if (item == null || !item.isObject()) {
            return null;
        }
        String original = firstText(item, "original", "source", "error", "incorrect", "before");
        String corrected = firstText(item, "corrected", "correction", "revised", "fixed", "suggested", "improved", "after", "rewrite");
        String explanation = firstText(item, "explanation", "reason", "comment", "note", "why");
        if (original.isBlank() && corrected.isBlank()) {
            return null;
        }
        if (original.isBlank()) {
            original = corrected;
        }
        if (corrected.isBlank()) {
            corrected = suggestCorrection(original);
        }
        if (explanation.isBlank()) {
            explanation = suggestExplanation(original, corrected);
        }
        return correctedExample(original, corrected, explanation);
    }

    private List<ObjectNode> defaultCorrectedExamples(AssessmentSkill skill) {
        if (skill == AssessmentSkill.SPEAKING) {
            return List.of(
                    correctedExample(
                            "I am agree that family still important in city life.",
                            "I agree that family is still important in city life.",
                            "Không dùng am agree; thêm động từ to be trước important."
                    ),
                    correctedExample(
                            "In the future I will more confident when I speak.",
                            "In the future I will be more confident when I speak.",
                            "Cần be sau will trước tính từ confident."
                    )
            );
        }
        if (skill == AssessmentSkill.VOCABULARY) {
            return List.of(
                    correctedExample(
                            "This policy can effect the daily life of students.",
                            "This policy can affect the daily life of students.",
                            "Affect là động từ; effect thường dùng như danh từ."
                    ),
                    correctedExample(
                            "She made a research about climate change.",
                            "She conducted research on climate change.",
                            "Research không đi với make; dùng conduct/do research on."
                    )
            );
        }
        if (skill == AssessmentSkill.LISTENING || skill == AssessmentSkill.READING) {
            return List.of(
                    correctedExample(
                            "writting / recieve",
                            "writing / receive",
                            "Giữ đúng chính tả vì một lỗi spelling cũng làm mất điểm."
                    ),
                    correctedExample(
                            "answers copied without checking word limit",
                            "answers checked against the word limit before submission",
                            "Đọc lại instruction về số từ trước khi chốt đáp án."
                    )
            );
        }
        return List.of(
                correctedExample(
                        "The diagram illustrate the process of making ethanol.",
                        "The diagram illustrates the process of making ethanol.",
                        "Chủ ngữ số ít the diagram cần động từ thêm -s."
                ),
                correctedExample(
                        "The milled corn is cooked for four hours after water has been added.",
                        "After water is added, the milled corn is cooked for four hours.",
                        "Khi mô tả quy trình, hãy đưa mốc thời gian lên đầu câu và dùng hiện tại đơn."
                )
        );
    }

    private String suggestCorrection(String original) {
        String next = original == null ? "" : original.trim();
        if (next.isBlank()) {
            return "";
        }
        next = next.replaceAll("(?i)\\bafter water has been added\\b", "after water is added");
        next = next.replaceAll("(?i)\\bhas been added\\b", "is added");
        next = next.replaceAll("(?i)\\billustrate the\\b", "illustrates the");
        next = next.replaceAll("(?i)^There is many\\b", "There are many");
        next = next.replaceAll("(?i)\\bI am agree\\b", "I agree");
        next = next.replaceAll("(?i)\\bwill more\\b", "will be more");
        next = next.replaceAll("(?i)\\bcan effect\\b", "can affect");
        next = next.replaceAll("(?i)\\bmade a research\\b", "conducted research");
        next = next.replaceAll("(?i)\\bwritting\\b", "writing");
        next = next.replaceAll("(?i)\\brecieve\\b", "receive");
        next = next.replaceAll("(?i)\\bdepend on both\\b", "depends on both");
        if (next.matches("(?i)The milled corn is cooked for four hours after water is added\\.?")) {
            return "After water is added, the milled corn is cooked for four hours.";
        }
        if (next.equals(original.trim())) {
            return next.replaceFirst("(?i)\\bafter\\b", "once");
        }
        return next;
    }

    private String suggestExplanation(String original, String corrected) {
        String source = (original + " " + corrected).toLowerCase();
        if (source.contains("water is added") || source.contains("milled corn")) {
            return "Khi mô tả quy trình, hãy đưa mốc thời gian lên đầu câu và dùng hiện tại đơn thay vì present perfect.";
        }
        if (source.contains("illustrates")) {
            return "Chủ ngữ số ít the diagram cần động từ thêm -s.";
        }
        if (source.contains("there are many")) {
            return "Many people là số nhiều nên dùng there are.";
        }
        if (source.contains("i agree")) {
            return "Không dùng am agree; agree đã là động từ.";
        }
        return "Giữ thì hiện tại đơn, sửa collocation và đưa thông tin thời gian/điều kiện lên vị trí tự nhiên hơn.";
    }

    private ObjectNode correctedExample(String original, String corrected, String explanation) {
        ObjectNode example = OBJECT_MAPPER.createObjectNode();
        example.put("original", original);
        example.put("corrected", corrected);
        example.put("explanation", explanation);
        return example;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = textValue(node, field);
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private String textValue(JsonNode node, String field) {
        if (node == null || !node.has(field) || node.get(field).isNull()) {
            return "";
        }
        return node.get(field).asText("").trim();
    }

    private String objectivePart(AssessmentSkill skill, int questionNumber) {
        if (skill == AssessmentSkill.LISTENING) {
            return "part_" + Math.min(4, ((questionNumber - 1) / 10) + 1);
        }
        if (questionNumber <= 13) return "part_1";
        if (questionNumber <= 26) return "part_2";
        return "part_3";
    }

    private byte[] loadDemoSpeakingAudio() {
        List<Path> candidates = List.of(
                Paths.get("frontend", "public", "sheet-speaking", "sample-answer.wav"),
                Paths.get("..", "frontend", "public", "sheet-speaking", "sample-answer.wav")
        );
        for (Path candidate : candidates) {
            try {
                Path normalized = candidate.toAbsolutePath().normalize();
                if (Files.exists(normalized)) {
                    return Files.readAllBytes(normalized);
                }
            } catch (Exception exception) {
                log.warn("Không thể đọc file Speaking demo tại {}: {}", candidate, exception.getMessage());
            }
        }
        log.warn("Không tìm thấy file Speaking demo; bài Speaking sẽ dùng transcript và đánh giá dự phòng.");
        return null;
    }

    private String stageDemoSpeakingAudio(byte[] audioBytes) {
        try {
            Path directory = Paths.get(assessmentAudioDirectory).toAbsolutePath().normalize();
            Files.createDirectories(directory);
            Files.write(directory.resolve(DEMO_AUDIO_FILE), audioBytes);
            return "/api/student/assessments/audio/" + DEMO_AUDIO_FILE;
        } catch (Exception exception) {
            log.warn("Không thể lưu file Speaking demo: {}", exception.getMessage());
            return null;
        }
    }

    private String writingAnswer() {
        return """
                Task 1

                The diagram illustrates the process used to manufacture ethanol fuel from corn. Overall, it is a linear process that begins with storing and milling corn and ends with the storage and transportation of the finished fuel. Water is introduced during cooking, while a lengthy fermentation stage takes place before the liquid is purified.

                First, harvested corn is placed in storage and then sent to a mill, where it is ground into smaller particles. The milled corn is cooked for four hours after water has been added. Once cooking is complete, the mixture is transferred to a fermentation tank and remains there for forty-eight hours. The fermented material is then separated into two products: a solid by-product and a liquid. Only the liquid continues through the fuel-production process. It is purified for five hours to remove remaining impurities and produce ethanol of the required quality. Finally, the ethanol is stored in suitable containers before being transported to its destination.

                Task 2

                Success in sport depends on both the body and the mind. Some people argue that physical strength is the decisive factor because athletes must produce speed, power and endurance. Others believe mental strength matters more because competition creates pressure that must be controlled. Although physical preparation is essential, I believe mental resilience is ultimately the more important quality at elite level.

                Physical ability clearly determines whether a person can perform the basic demands of a sport. A marathon runner needs cardiovascular endurance, while a weightlifter requires exceptional muscular power. Technical skill can also deteriorate when an athlete becomes tired. For this reason, systematic training, recovery and nutrition form the foundation of performance. Without that foundation, confidence alone cannot enable an athlete to compete successfully.

                However, athletes of a similar physical standard are often separated by their mental response to difficult moments. A tennis player may have excellent technique but still lose after becoming frustrated by one mistake. In contrast, a mentally resilient competitor can remain calm, adjust tactics and continue making sensible decisions. Mental strength also supports the discipline required for years of training and helps athletes recover from injury or defeat.

                In my view, physical strength allows an athlete to enter the competition, but mental strength often decides the result. The best performers combine both qualities, yet the mind is what enables them to use their physical capacity consistently under pressure. Coaches should therefore develop psychological skills such as concentration, emotional control and realistic goal setting alongside fitness.

                In conclusion, physical conditioning is indispensable in sport, but mental resilience has greater importance when athletes are closely matched. It transforms preparation into reliable performance and helps competitors respond constructively to pressure and setbacks.
                """;
    }

    private String speakingAnswer(CourseAssessment assessment) {
        String title = assessment.getTitle() == null ? "" : assessment.getTitle().toLowerCase(Locale.ROOT);
        if (title.contains("100 ielts speaking questions")) {
            return """
                    Part 1: I come from Hanoi, and what I appreciate most is the combination of old neighbourhoods and modern services. I am currently a university student. I chose my course because I enjoy solving practical problems with technology. In my free time, I usually watch documentary films with friends because discussing them helps me notice details I might otherwise miss.

                    I do some shopping online, especially for books, but I still prefer local shops for clothes because I can check the quality. Online shopping has clearly become more popular because it saves time, although delivery and product quality can sometimes be unreliable.
                    """;
        }
        return """
                Part 1: I am from Hanoi and I have lived there for most of my life. I enjoy the city because it is energetic and convenient, although traffic can be stressful. I currently live with my family near my university. I often watch films at weekends, particularly documentaries and character-driven dramas.

                Part 2: An activity I enjoy doing alone is cycling around West Lake early in the morning. I normally do it once or twice a week. The route is long enough to be challenging, but the quiet streets and fresh air make it relaxing. Cycling alone gives me time to organise my thoughts without checking messages or following a fixed schedule. I usually stop briefly to take photographs or have a drink. Afterward, I feel more focused and energetic for the rest of the day.

                Part 3: People should not focus only on work because rest and personal interests protect their health and can improve productivity. Parents can introduce children to useful activities, but they should also allow children to make choices. Younger people now spend more leisure time online than previous generations, while older people were more likely to join local outdoor activities. Nevertheless, both generations still need meaningful social contact and a healthy balance between responsibility and relaxation.
                """;
    }

    private String reflectionAnswer() {
        return """
                Sau sáu mô-đun, lỗi chính của tôi từng nằm ở spelling trong Listening, đọc quá lâu ở Matching Headings và thiếu ví dụ cụ thể khi nói Part 3. Tôi đã sửa bằng cách kiểm tra dạng từ trước khi chuyển đáp án, giới hạn thời gian đọc mỗi đoạn và dùng cấu trúc quan điểm - lý do - ví dụ cho câu trả lời Speaking. Bài Writing hiện có bố cục rõ hơn, nhưng tôi vẫn cần rà soát mạo từ và tránh lặp từ. Mục tiêu tiếp theo là làm một đề bốn kỹ năng có bấm giờ mỗi tuần, ghi lại lỗi theo dạng câu hỏi và ôn lại Module 2, Module 4 trước kỳ thi thử tiếp theo.
                """;
    }
}
