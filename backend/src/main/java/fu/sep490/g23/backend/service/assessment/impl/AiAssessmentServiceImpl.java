package fu.sep490.g23.backend.service.assessment.impl;
import fu.sep490.g23.backend.dto.response.assessment.RubricCriterionResponse;
import fu.sep490.g23.backend.service.assessment.IeltsBandScale;
import fu.sep490.g23.backend.entity.assessment.enums.AiEvaluationMode;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.dto.response.assessment.AiAssessmentSubmissionResponse;
import fu.sep490.g23.backend.service.assessment.AssessmentPassingThresholdResolver;
import fu.sep490.g23.backend.dto.response.assessment.CourseAssessmentResponse;
import fu.sep490.g23.backend.dto.response.assessment.AssessmentRubricResponse;
import fu.sep490.g23.backend.service.assessment.AiAssessmentService;
import fu.sep490.g23.backend.entity.assessment.enums.SubmissionStatus;
import fu.sep490.g23.backend.service.assessment.AssessmentAudioStorageService;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fu.sep490.g23.backend.dto.request.assessment.AssessmentSubmissionRequest;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.AssessmentSubmission;
import fu.sep490.g23.backend.entity.assessment.CourseAssessment;
import fu.sep490.g23.backend.entity.assessment.RubricCriterion;
import fu.sep490.g23.backend.entity.course.OnlineCourseModule;
import fu.sep490.g23.backend.entity.course.OnlineLesson;
import fu.sep490.g23.backend.entity.course.OnlineCourse;
import fu.sep490.g23.backend.entity.course.OnlineCourseEnrollment;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.ai.AiEvaluationResult;
import fu.sep490.g23.backend.service.course.CourseProgressService;
import fu.sep490.g23.backend.service.course.CourseEnrollmentAccessPolicy;
import fu.sep490.g23.backend.service.course.OnlineCourseVersionService;
import fu.sep490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sep490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sep490.g23.backend.repository.course.OnlineCourseEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class AiAssessmentServiceImpl implements AiAssessmentService {
    private static final Pattern VOCABULARY_HEADING = Pattern.compile("(?m)^###\\s+\\d+\\.\\s+(.+)$");
    private static final BigDecimal VOCABULARY_OFF_TOPIC_CAP_RATIO = BigDecimal.valueOf(0.35);
    private static final String UI_CONFIG_MARKER = "\n\n[ENGLISHLAB_UI_CONFIG]\n";
    private static final Pattern SPEAKING_DURATION_PATTERN = Pattern.compile("Recording duration seconds:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SPEAKING_WORD_COUNT_PATTERN = Pattern.compile("Transcript word count:\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    private final CourseAssessmentRepository courseAssessmentRepository;
    private final AssessmentSubmissionRepository submissionRepository;
    private final OnlineCourseRepository onlineCourseRepository;
    private final OnlineCourseEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final AiEvaluationClient aiEvaluationClient;
    private final AssessmentAudioStorageService assessmentAudioStorageService;
    private final CourseProgressService courseProgressService;
    private final CourseEnrollmentAccessPolicy courseEnrollmentAccessPolicy;
    private final OnlineCourseVersionService onlineCourseVersionService;
    private final AssessmentPassingThresholdResolver passingThresholdResolver;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public List<CourseAssessmentResponse> getCourseAssessments(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail).orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = onlineCourseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Course not found"));
        OnlineCourseEnrollment enrollment = ensureEnrolled(student, course);
        List<CourseAssessment> assessments = courseAssessmentRepository
                .findAllById(onlineCourseVersionService.getLatestPublishedAssessmentIds(enrollment)).stream()
                .filter(assessment -> assessment.getOnlineCourse().getId().equals(course.getId()))
                .sorted(Comparator.comparing(CourseAssessment::getDisplayOrder).thenComparing(CourseAssessment::getId))
                .toList();
        return assessments
                .stream()
                .map(assessment -> toResponse(assessment, student))
                .toList();
    }

    @Override
    public AiAssessmentSubmissionResponse submitAssessment(Long assessmentId, AssessmentSubmissionRequest request, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail).orElseThrow(() -> new RuntimeException("Student not found"));
        CourseAssessment assessment = courseAssessmentRepository.findById(assessmentId).orElseThrow(() -> new RuntimeException("Assessment not found"));
        normalizeAssessmentRubricCompatibility(assessment);
        OnlineCourseEnrollment enrollment = ensureEnrolled(student, assessment.getOnlineCourse());
        onlineCourseVersionService.assertAssessmentBelongsToEnrollment(enrollment, assessmentId);

        if (assessment.getAiEvaluationMode() == AiEvaluationMode.NONE) {
            throw new RuntimeException("Bài đánh giá này chưa bật phản hồi tự động.");
        }
        if (!hasSubmissionContent(request)) {
            throw new RuntimeException("Vui lòng nhập nội dung bài làm trước khi nộp.");
        }
        validateSkillAssessmentConfiguration(assessment);

        var speakingAudio = resolveSpeakingAudio(assessment, request);
        String prompt = buildRubricPrompt(assessment, request, student, speakingAudio.isPresent());
        String submittedText = firstNonBlank(request.getSubmittedText(), request.getObjectiveAnswersJson());
        String targetVocabulary = extractTargetVocabulary(assessment.getModule());
        AiEvaluationResult aiResult;
        if (usesObjectiveAnswerKey(assessment, request)) {
            aiResult = evaluateObjectiveAssessment(assessment, request);
        } else if (isObjectiveAssessmentSkill(assessment.getSkill())) {
            aiResult = evaluateObjectiveAssessmentWithoutAnswerKey(assessment);
        } else if (isInsufficientWritingSubmission(assessment, request)) {
            aiResult = buildInsufficientWritingResult(assessment);
        } else {
            aiResult = speakingAudio
                    .map(audio -> aiEvaluationClient.evaluateWithAudio(prompt, audio.bytes(), audio.contentType()))
                    .orElseGet(() -> aiEvaluationClient.evaluate(prompt));
            aiResult = normalizeEvaluationResult(aiResult, assessment);
            aiResult = applyVocabularyRelevanceGuard(aiResult, assessment, submittedText, targetVocabulary);
            aiResult = applySpeakingEvidenceGuard(aiResult, assessment, request);
        }
        aiResult = normalizeEvaluationResult(aiResult, assessment);
        BigDecimal score = aiResult.getEstimatedScore();
        SubmissionStatus status = resolveSubmissionStatus(score, assessment);

        AssessmentSubmission submission = AssessmentSubmission.builder()
                .assessment(assessment)
                .student(student)
                .submittedText(request.getSubmittedText())
                .submittedAudioUrl(request.getSubmittedAudioUrl())
                .objectiveAnswersJson(request.getObjectiveAnswersJson())
                .fullscreenExitCount(request.getFullscreenExitCount())
                .tabSwitchCount(request.getTabSwitchCount())
                .microphoneChecked(request.getMicrophoneChecked())
                .deviceCheckPassed(request.getDeviceCheckPassed())
                .aiScore(score)
                .aiFeedbackJson(aiResult.getFeedbackJson())
                .aiPromptSnapshot(prompt)
                .aiProvider(aiResult.getProvider())
                .aiModel(aiResult.getModel())
                .aiRawResponse(aiResult.getRawResponse())
                .status(status)
                .build();

        AssessmentSubmission savedSubmission = submissionRepository.save(submission);
        enrollmentRepository.findByStudentAndOnlineCourse(student, assessment.getOnlineCourse())
                .ifPresent(activeEnrollment -> courseProgressService.refreshEnrollmentProgress(
                        activeEnrollment,
                        assessment.getOnlineCourse(),
                        student
                ));
        return toSubmissionResponse(savedSubmission);
    }

    private OnlineCourseEnrollment ensureEnrolled(User student, OnlineCourse course) {
        return courseEnrollmentAccessPolicy.requireAssessmentAccess(student, course);
    }

    private void validateSkillAssessmentConfiguration(CourseAssessment assessment) {
        if (assessment.getSkill() == AssessmentSkill.WRITING) {
            if (assessment.getRubric() == null || assessment.getRubric().getSkill() != AssessmentSkill.WRITING) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Bài Writing này chưa có bộ tiêu chí chấm phù hợp. Bài làm của bạn vẫn đang được giữ trong bản nháp."
                );
            }
        }
        if (assessment.getSkill() == AssessmentSkill.SPEAKING) {
            if (assessment.getRubric() == null || assessment.getRubric().getSkill() != AssessmentSkill.SPEAKING) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Bài Speaking này chưa có bộ tiêu chí chấm phù hợp. Bài làm của bạn vẫn đang được giữ trong bản nháp."
                );
            }
        }
        if (isObjectiveAssessmentSkill(assessment.getSkill()) && assessment.getRubric() != null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Bài nghe hoặc bài đọc không dùng bộ tiêu chí viết. Hãy kiểm tra lại cấu hình bài đánh giá."
            );
        }
    }

    private SubmissionStatus resolveSubmissionStatus(BigDecimal score, CourseAssessment assessment) {
        if (assessment.getAiEvaluationMode() == AiEvaluationMode.EXPLAIN_ONLY) {
            return SubmissionStatus.AI_EVALUATED;
        }
        if (score == null) {
            return SubmissionStatus.AI_EVALUATED;
        }

        BigDecimal passingThreshold = passingThresholdResolver.resolve(assessment);
        if (passingThreshold == null) {
            return SubmissionStatus.AI_EVALUATED;
        }

        return score.compareTo(passingThreshold) >= 0
                ? SubmissionStatus.PASSED
                : SubmissionStatus.NEEDS_IMPROVEMENT;
    }

    private boolean usesObjectiveAnswerKey(CourseAssessment assessment, AssessmentSubmissionRequest request) {
        if (assessment == null || request == null) {
            return false;
        }
        if (!isObjectiveAssessmentSkill(assessment.getSkill())) {
            return false;
        }
        return hasText(assessment.getObjectiveAnswerKey()) && hasText(request.getObjectiveAnswersJson());
    }

    private boolean isObjectiveAssessmentSkill(AssessmentSkill skill) {
        return skill == AssessmentSkill.LISTENING || skill == AssessmentSkill.READING;
    }

    private AiEvaluationResult evaluateObjectiveAssessmentWithoutAnswerKey(CourseAssessment assessment) {
        ObjectNode feedback = objectMapper.createObjectNode();
        feedback.put("skill", assessment.getSkill().name());
        feedback.putNull("estimatedScore");
        feedback.put("estimatedBand", "");
        feedback.put("isOfficialScore", false);
        feedback.put("summary", "Bài làm đã được lưu, nhưng bài đánh giá này chưa có đáp án chuẩn để chấm tự động.");
        feedback.set("criteria", objectMapper.createArrayNode());
        feedback.set("strengths", objectMapper.createArrayNode());
        feedback.set("weaknesses", objectMapper.createArrayNode()
                .add("Chưa thể tính số câu đúng hoặc band vì thiếu đáp án chuẩn của bài thi."));
        feedback.set("suggestions", objectMapper.createArrayNode()
                .add("Hãy kiểm tra lại cấu hình đáp án chuẩn trước khi dùng bài này để chấm điểm."));
        feedback.set("recommendedReview", objectMapper.createArrayNode());
        feedback.set("partFeedback", objectMapper.createArrayNode());
        feedback.set("weakQuestionTypes", objectMapper.createArrayNode());
        feedback.set("mistakeAnalysis", objectMapper.createArrayNode());
        feedback.set("correctedExamples", objectMapper.createArrayNode());
        feedback.put("disclaimer", "Đây là phản hồi hỗ trợ học tập, không phải điểm IELTS chính thức.");
        try {
            return AiEvaluationResult.builder()
                    .estimatedScore(null)
                    .feedbackJson(objectMapper.writeValueAsString(feedback))
                    .provider("EnglishLab")
                    .model("missing-objective-answer-key")
                    .rawResponse("Objective answer key is missing; no AI scoring was performed.")
                    .audioInputAnalyzed(false)
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to create objective fallback feedback", ex);
        }
    }

    private AiEvaluationResult evaluateObjectiveAssessment(CourseAssessment assessment, AssessmentSubmissionRequest request) {
        try {
            JsonNode answerKeyRoot = objectMapper.readTree(assessment.getObjectiveAnswerKey());
            JsonNode submissionRoot = objectMapper.readTree(request.getObjectiveAnswersJson());
            ObjectiveEvaluationSummary summary = scoreObjectiveAssessment(assessment, answerKeyRoot, submissionRoot);
            ObjectNode feedback = buildObjectiveFeedback(assessment, summary);
            return AiEvaluationResult.builder()
                    .estimatedScore(BigDecimal.valueOf(summary.correctCount))
                    .feedbackJson(objectMapper.writeValueAsString(feedback))
                    .provider("EnglishLab")
                    .model("objective-answer-key")
                    .rawResponse("Deterministic objective scoring from stored answer key")
                    .audioInputAnalyzed(false)
                    .build();
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to evaluate objective assessment", ex);
        }
    }

    private ObjectiveEvaluationSummary scoreObjectiveAssessment(CourseAssessment assessment, JsonNode answerKeyRoot, JsonNode submissionRoot) {
        ObjectiveEvaluationSummary summary = new ObjectiveEvaluationSummary();
        if (submissionRoot == null || !submissionRoot.isObject()) {
            return summary;
        }

        JsonNode responses = submissionRoot.path("responses");
        if (!responses.isArray()) {
            return summary;
        }

        for (JsonNode response : responses) {
            String partKey = response.path("part").asText("");
            String partLabel = prettyPartLabel(partKey);
            ObjectivePartSummary partSummary = summary.parts.computeIfAbsent(partKey, key -> new ObjectivePartSummary(partKey, partLabel));
            String answerType = response.path("answerType").asText("");
            String questionNumber = response.path("questionNumber").asText("");
            String studentAnswer = response.path("answer").asText("");

            if ("multi_select_letters".equals(answerType)) {
                Set<String> expected = resolveExpectedAnswerSet(answerKeyRoot, questionNumber);
                Set<String> selected = parseAnswerSet(studentAnswer);
                Set<String> correctSelected = new LinkedHashSet<>(selected);
                correctSelected.retainAll(expected);
                Set<String> missing = new LinkedHashSet<>(expected);
                missing.removeAll(selected);
                Set<String> extra = new LinkedHashSet<>(selected);
                extra.removeAll(expected);

                int earned = correctSelected.size();
                int total = expected.size();
                summary.correctCount += earned;
                summary.totalCount += total;
                partSummary.correctCount += earned;
                partSummary.totalCount += total;

                if (earned == total && extra.isEmpty() && missing.isEmpty()) {
                    partSummary.strengths.add("Câu " + questionNumber + " đúng hoàn toàn.");
                } else {
                    partSummary.weaknesses.add(buildMultiSelectFeedback(questionNumber, expected, selected, missing, extra, earned, total));
                }
                continue;
            }

            List<String> expectedAnswers = resolveExpectedAnswers(answerKeyRoot, questionNumber);
            boolean correct = !expectedAnswers.isEmpty()
                    && expectedAnswers.stream()
                    .map(this::normalizeAnswer)
                    .anyMatch(expected -> expected.equals(normalizeAnswer(studentAnswer)));
            summary.totalCount += 1;
            partSummary.totalCount += 1;
            if (correct) {
                summary.correctCount += 1;
                partSummary.correctCount += 1;
                partSummary.strengths.add("Câu " + questionNumber + " đúng.");
            } else {
                String feedback = "Câu " + questionNumber + ": đáp án chấp nhận là "
                        + fallbackText(String.join(" / ", expectedAnswers))
                        + ", bài làm của bạn là " + fallbackText(studentAnswer) + ".";
                partSummary.weaknesses.add(feedback);
            }
        }

        return summary;
    }

    private ObjectNode buildObjectiveFeedback(CourseAssessment assessment, ObjectiveEvaluationSummary summary) {
        String estimatedBand = estimateObjectiveBand(assessment.getSkill(), summary.correctCount, summary.totalCount);
        ObjectNode root = objectMapper.createObjectNode();
        root.put("skill", assessment.getSkill().name());
        root.put("estimatedScore", summary.correctCount);
        root.put("estimatedBand", estimatedBand);
        root.put("isOfficialScore", false);
        root.put("rawScore", summary.correctCount);
        root.put("totalQuestions", summary.totalCount);
        root.put("correctCount", summary.correctCount);
        root.put("incorrectCount", Math.max(0, summary.totalCount - summary.correctCount));
        root.put("summary", "Hệ thống đã đối chiếu đáp án của bạn với đáp án chuẩn đã lưu cho bài thi.");

        ArrayNode criteria = objectMapper.createArrayNode();
        ArrayNode strengths = objectMapper.createArrayNode();
        ArrayNode weaknesses = objectMapper.createArrayNode();
        ArrayNode suggestions = objectMapper.createArrayNode();
        ArrayNode recommendedReview = objectMapper.createArrayNode();
        ArrayNode partFeedback = objectMapper.createArrayNode();
        ArrayNode weakQuestionTypes = objectMapper.createArrayNode();
        ArrayNode mistakeAnalysis = objectMapper.createArrayNode();
        ArrayNode sourceSignals = objectMapper.createArrayNode();
        ArrayNode correctedExamples = objectMapper.createArrayNode();
        ObjectNode originality = objectMapper.createObjectNode();

        summary.parts.values().forEach(partSummary -> {
            String summaryText = "Đúng " + partSummary.correctCount + "/" + partSummary.totalCount + " câu ở " + partSummary.partLabel + ".";
            ObjectNode criterion = createCriterionNode(partSummary.partLabel, partSummary.correctCount, summaryText);
            criteria.add(criterion);

            if (partSummary.correctCount > 0) {
                strengths.add("Làm tốt " + partSummary.partLabel.toLowerCase(Locale.ROOT) + " với " + partSummary.correctCount + " câu đúng.");
            }
            if (partSummary.weaknesses.isEmpty() && partSummary.correctCount < partSummary.totalCount) {
                partSummary.weaknesses.add("Phần này chưa có lỗi rõ rệt để phân tích thêm.");
            }

            ObjectNode partNode = objectMapper.createObjectNode();
            partNode.put("partKey", partSummary.partKey);
            partNode.put("partLabel", partSummary.partLabel);
            partNode.put("summary", summaryText);
            partNode.set("strengths", listToArrayNode(partSummary.strengths));
            partNode.set("weaknesses", listToArrayNode(partSummary.weaknesses));
            partNode.set("suggestions", listToArrayNode(buildPartSuggestions(partSummary)));
            partFeedback.add(partNode);

            if (partSummary.correctCount < partSummary.totalCount) {
                weaknesses.add("Cần ôn lại " + partSummary.partLabel + " vì còn " + (partSummary.totalCount - partSummary.correctCount) + " câu chưa đúng.");
                suggestions.add("Xem lại đáp án chuẩn của " + partSummary.partLabel + " rồi làm lại phần này.");
                recommendedReview.add(partSummary.partLabel);
                weakQuestionTypes.add(partSummary.partLabel);
                partSummary.weaknesses.forEach(mistakeAnalysis::add);
            }
        });

        if (summary.parts.isEmpty()) {
            weaknesses.add("Chưa có dữ liệu bài làm để đối chiếu với đáp án chuẩn.");
            suggestions.add("Hãy nhập đủ đáp án trước khi nộp.");
        }

        if (!strengths.isEmpty()) {
            root.set("strengths", strengths);
        } else {
            root.set("strengths", objectMapper.createArrayNode());
        }
        root.set("weaknesses", weaknesses);
        root.set("suggestions", suggestions);
        root.set("recommendedReview", recommendedReview);
        root.set("partFeedback", partFeedback);
        root.set("criteria", criteria);
        root.set("weakQuestionTypes", weakQuestionTypes);
        root.set("mistakeAnalysis", mistakeAnalysis);
        root.set("correctedExamples", correctedExamples);
        root.put("plagiarismRisk", "LOW");
        root.put("aiUsageRisk", "LOW");
        root.set("sourceSignals", sourceSignals.add("Đối chiếu trực tiếp từng câu trả lời với đáp án chuẩn được lưu trong đề."));
        originality.put("summary", "Bài làm là bài trả lời khách quan nên hệ thống đối chiếu trực tiếp với đáp án chuẩn thay vì suy đoán theo văn phong.");
        originality.put("plagiarismRisk", "LOW");
        originality.put("aiUsageRisk", "LOW");
        root.set("originalityAnalysis", originality);
        root.put("disclaimer", "Đây là phản hồi hỗ trợ học tập, không phải điểm IELTS chính thức.");
        return root;
    }

    private String estimateObjectiveBand(AssessmentSkill skill, int correctCount, int totalCount) {
        if (totalCount <= 0) {
            return "";
        }
        int raw = Math.max(0, Math.min(correctCount, 40));
        if (skill == AssessmentSkill.LISTENING) {
            if (raw >= 39) return "9.0";
            if (raw >= 37) return "8.5";
            if (raw >= 35) return "8.0";
            if (raw >= 32) return "7.5";
            if (raw >= 30) return "7.0";
            if (raw >= 26) return "6.5";
            if (raw >= 23) return "6.0";
            if (raw >= 18) return "5.5";
            if (raw >= 16) return "5.0";
            if (raw >= 13) return "4.5";
            if (raw >= 10) return "4.0";
            if (raw >= 8) return "3.5";
            if (raw >= 6) return "3.0";
            if (raw >= 4) return "2.5";
            return "";
        }
        if (skill == AssessmentSkill.READING) {
            if (raw >= 39) return "9.0";
            if (raw >= 37) return "8.5";
            if (raw >= 35) return "8.0";
            if (raw >= 33) return "7.5";
            if (raw >= 30) return "7.0";
            if (raw >= 27) return "6.5";
            if (raw >= 23) return "6.0";
            if (raw >= 19) return "5.5";
            if (raw >= 15) return "5.0";
            if (raw >= 13) return "4.5";
            if (raw >= 10) return "4.0";
            if (raw >= 8) return "3.5";
            if (raw >= 6) return "3.0";
            if (raw >= 4) return "2.5";
        }
        return "";
    }

    private ArrayNode listToArrayNode(List<String> values) {
        ArrayNode array = objectMapper.createArrayNode();
        if (values == null) {
            return array;
        }
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .forEach(array::add);
        return array;
    }

    private List<String> buildPartSuggestions(ObjectivePartSummary partSummary) {
        List<String> suggestions = new ArrayList<>();
        if (partSummary.correctCount == partSummary.totalCount) {
            suggestions.add("Giữ nhịp làm bài ổn định và tiếp tục luyện phần này để duy trì độ chính xác.");
            return suggestions;
        }
        suggestions.add("Ôn lại các câu sai trong " + partSummary.partLabel + " và đối chiếu với đáp án chuẩn.");
        suggestions.add("Làm lại " + partSummary.partLabel + " sau khi xem lại những chỗ dễ nhầm.");
        return suggestions;
    }

    private Set<String> resolveExpectedAnswerSet(JsonNode answerKeyRoot, String questionNumber) {
        Set<String> expected = new LinkedHashSet<>();
        if (answerKeyRoot == null || !answerKeyRoot.isObject()) {
            return expected;
        }

        JsonNode directNode = answerKeyRoot.path(questionNumber);
        if (directNode.isArray()) {
            directNode.forEach(item -> {
                String value = normalizeLetter(item.asText(""));
                if (!value.isBlank()) {
                    expected.add(value);
                }
            });
            return expected;
        }
        if (directNode.isTextual()) {
            String value = normalizeLetter(directNode.asText(""));
            if (!value.isBlank()) {
                expected.add(value);
            }
            return expected;
        }

        Arrays.stream(String.valueOf(questionNumber).split("-"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .forEach(number -> {
                    JsonNode node = answerKeyRoot.path(number);
                    if (node.isArray()) {
                        node.forEach(item -> {
                            String value = normalizeLetter(item.asText(""));
                            if (!value.isBlank()) {
                                expected.add(value);
                            }
                        });
                    } else {
                        String value = normalizeLetter(node.asText(""));
                        if (!value.isBlank()) {
                            expected.add(value);
                        }
                    }
        });
        return expected;
    }

    private Set<String> parseAnswerSet(String studentAnswer) {
        Set<String> selected = new LinkedHashSet<>();
        if (studentAnswer == null || studentAnswer.isBlank()) {
            return selected;
        }
        Arrays.stream(studentAnswer.split("[,\\s]+"))
                .map(this::normalizeLetter)
                .filter(value -> !value.isBlank())
                .forEach(selected::add);
        return selected;
    }

    private List<String> resolveExpectedAnswers(JsonNode answerKeyRoot, String questionNumber) {
        List<String> expectedAnswers = new ArrayList<>();
        if (answerKeyRoot == null || !answerKeyRoot.isObject()) {
            return expectedAnswers;
        }
        JsonNode directNode = answerKeyRoot.path(questionNumber);
        if (directNode.isMissingNode() || directNode.isNull()) {
            return expectedAnswers;
        }
        if (directNode.isArray()) {
            directNode.forEach(item -> {
                String value = item.asText("").trim();
                if (!value.isBlank()) {
                    expectedAnswers.add(value);
                }
            });
            return expectedAnswers;
        }
        String value = directNode.asText("").trim();
        if (!value.isBlank()) {
            expectedAnswers.add(value);
        }
        return expectedAnswers;
    }

    private String buildMultiSelectFeedback(String questionNumber, Set<String> expected, Set<String> selected, Set<String> missing, Set<String> extra, int earned, int total) {
        List<String> parts = new ArrayList<>();
        parts.add("Câu " + questionNumber + ": đúng " + earned + "/" + total + " đáp án.");
        if (!missing.isEmpty()) {
            parts.add("Thiếu " + String.join(", ", missing) + ".");
        }
        if (!extra.isEmpty()) {
            parts.add("Đã chọn thừa " + String.join(", ", extra) + ".");
        }
        parts.add("Đáp án chuẩn: " + String.join(", ", expected) + ".");
        return String.join(" ", parts);
    }

    private String normalizeAnswer(String value) {
        return String.valueOf(value == null ? "" : value)
                .toLowerCase(Locale.ROOT)
                .replaceAll("\\s+", " ")
                .replaceAll("[\\p{Punct}&&[^-]]+", "")
                .trim();
    }

    private String normalizeLetter(String value) {
        return String.valueOf(value == null ? "" : value).trim().toUpperCase(Locale.ROOT);
    }

    private String fallbackText(String value) {
        return hasText(value) ? value.trim() : "chưa trả lời";
    }

    private String prettyPartLabel(String partKey) {
        if (partKey == null || partKey.isBlank()) {
            return "Phần thi";
        }
        String normalized = partKey.replace("part_", "").trim();
        return "Part " + normalized;
    }

    private static final class ObjectiveEvaluationSummary {
        private int correctCount;
        private int totalCount;
        private final Map<String, ObjectivePartSummary> parts = new LinkedHashMap<>();
    }

    private static final class ObjectivePartSummary {
        private final String partKey;
        private final String partLabel;
        private int correctCount;
        private int totalCount;
        private final List<String> strengths = new ArrayList<>();
        private final List<String> weaknesses = new ArrayList<>();

        private ObjectivePartSummary(String partKey, String partLabel) {
            this.partKey = partKey;
            this.partLabel = partLabel;
        }
    }

    private java.util.Optional<AssessmentAudioStorageService.StoredAssessmentAudio> resolveSpeakingAudio(CourseAssessment assessment, AssessmentSubmissionRequest request) {
        if (assessment.getSkill() != AssessmentSkill.SPEAKING || request == null || !hasText(request.getSubmittedAudioUrl())) {
            return java.util.Optional.empty();
        }
        return assessmentAudioStorageService.loadStoredAudioFromUrl(request.getSubmittedAudioUrl());
    }

    private String buildRubricPrompt(CourseAssessment assessment, AssessmentSubmissionRequest request, User student, boolean hasAnalyzableAudio) {
        StringBuilder criteriaText = new StringBuilder();
        if (assessment.getRubric() != null) {
            assessment.getRubric().getCriteria().stream()
                    .sorted(Comparator.comparing(RubricCriterion::getDisplayOrder).thenComparing(RubricCriterion::getId))
                    .forEach(criterion -> criteriaText.append("- ")
                            .append(criterion.getName()).append(" (").append(criterion.getWeight()).append("%): ")
                            .append(criterion.getDescription() == null ? "" : criterion.getDescription())
                            .append("\nBand/level descriptors: ").append(criterion.getBandDescriptors() == null ? "" : criterion.getBandDescriptors())
                            .append("\n"));
        }

        OnlineCourse course = assessment.getOnlineCourse();
        String submittedContent = buildSubmittedContent(request, assessment.getSkill(), hasAnalyzableAudio);
        String targetVocabulary = extractTargetVocabulary(assessment.getModule());
        String submissionGuidance = skillSubmissionGuidance(assessment.getSkill());
        String skillEvaluationPolicy = skillEvaluationPolicy(assessment, hasAnalyzableAudio);
        String speakingAudioState = hasAnalyzableAudio
                ? "Actual speaking audio is attached in this Gemini request. You must listen to it and use it as primary evidence for pronunciation, fluency, pacing, pauses, stress, intonation, and spoken delivery."
                : "No actual audio bytes are attached. Treat any audio URL as a reference only.";

        return """
                You are EnglishLab AI Evaluator for a self-paced online English course.
                This course has no direct teacher, so your job is to give reliable formative feedback.
                Evaluate the student submission ONLY according to the provided rubric, course target band, course target outcome, module objective, and assessment instructions.

                Critical rules:
                - The learner-facing feedback must be primarily in Vietnamese.
                - Keep criterion names in their original rubric language, but write criterion feedback in Vietnamese.
                - strengths, weaknesses, suggestions, recommendedReview, sourceSignals, and originalityAnalysis.summary must be written in natural Vietnamese.
                - correctedExamples.original and correctedExamples.corrected should stay in English when you are fixing English usage.
                - correctedExamples.explanation should be written in Vietnamese so Vietnamese learners understand the fix clearly.
                - Do not claim the result is an official IELTS, TOEIC, or other certification score.
                - Do not use peer grading.
                - If evidence is insufficient, explain that clearly inside the JSON feedback.
                - Return valid JSON only. Do not wrap it in markdown.
                - Evaluate only with the provided rubric criteria. Do not invent extra criteria.
                - estimatedScore should be numeric only when the assessment mode supports scoring and there is enough evidence; otherwise use null.
                - estimatedBand should be provided only when the rubric or course uses band-based evaluation and the evidence is sufficient; otherwise use an empty string.
                - When scoring on the IELTS band scale, use only whole or half bands (for example 6.0, 6.5, 7.0). Never use other decimal increments such as 6.3 or 7.2.
                - Give specific evidence from the student's submission, not generic advice.
                - Suggestions and recommendedReview must connect back to the course/module learning path.
                - correctedExamples must explain errors clearly for English learners.
                - Add an originality analysis based only on the submission text itself. Do not claim web search or exact source matching.
                - If the submission looks memorized, copied, template-heavy, or likely AI-assisted, report that as a risk signal with reasons.
                - If the learner explicitly states or the submission metadata indicates 100%% AI use, fully AI-generated work, or complete automated-tool use, aiUsageRisk must be HIGH.
                - originality or AI-use risk must be framed as non-final guidance for teachers, managers, or learners.
                - For vocabulary assessments, score harshly if the learner fails to use the target vocabulary of the module, uses unrelated vocabulary, or ignores the IELTS topic.
                - If the response is off-topic for the module or does not meaningfully use the target vocabulary list, Topic Relevance and Meaning Accuracy must be low, not maximum.
                - If AI mode is EXPLAIN_ONLY, do not invent a band score just to fill the schema. Prefer null estimatedScore unless there is an explicit objective answer key and enough evidence to justify a reliable numeric score.
                - For listening assessments, treat the submission as answers, notes, and an error log; do not evaluate it like an essay.
                - For reading assessments, treat the submission as answers, passage evidence, and an error log; do not evaluate it like an essay.
                - For speaking assessments, evaluate only the evidence that is actually present in the submission payload.
                - If actual speaking audio is attached, listen to the audio and evaluate pronunciation, fluency, pacing, pauses, stress, intonation, and delivery from the sound, not from transcript text alone.
                - If actual speaking audio is attached, infer the spoken wording from the audio when needed, but clearly lower confidence when the audio is unclear or noisy.
                - Do not pretend that you listened to the audio if the system only provides an audio URL or recording metadata instead of actual audio content.
                - If only an audio URL or recording metadata is provided, clearly state that pronunciation, exact wording, and meaning cannot be judged reliably from the current pipeline.
                - For speaking mock tests, if the submission includes part metadata, partFeedback is required and must include one separate entry for each available part such as Part 1, Part 2, and Part 3.
                - For speaking mock tests, suggestions and recommendedReview are also required so the learner can see Hành động tiếp theo and Nên ôn lại.
                - If a speaking response is empty, nearly empty, silent, or lacks meaningful evidence, say that directly, keep estimatedScore as null, and do not invent performance.
                - For writing assessments, evaluate the essay or paragraph as a writing response according to the writing rubric.

                Required JSON schema:
                {
                  "estimatedScore": null,
                  "estimatedBand": "",
                  "criteria": [
                    {
                      "name": "",
                      "score": 0,
                      "feedback": ""
                    }
                  ],
                  "strengths": ["..."],
                  "weaknesses": ["..."],
                  "suggestions": ["..."],
                  "recommendedReview": ["..."],
                  "partFeedback": [
                    {
                      "partKey": "part_1",
                      "partLabel": "Part 1",
                      "summary": "",
                      "strengths": ["..."],
                      "weaknesses": ["..."],
                      "suggestions": ["..."]
                    }
                  ],
                  "correctedExamples": [
                    {
                      "original": "",
                      "corrected": "",
                      "explanation": ""
                    }
                  ],
                  "plagiarismRisk": "LOW | MEDIUM | HIGH",
                  "aiUsageRisk": "LOW | MEDIUM | HIGH",
                  "originalityAnalysis": {
                    "summary": "",
                    "plagiarismRisk": "LOW | MEDIUM | HIGH",
                    "aiUsageRisk": "LOW | MEDIUM | HIGH"
                  },
                  "sourceSignals": ["..."],
                  "disclaimer": "Đây là phản hồi hỗ trợ học tập, không phải điểm IELTS chính thức."
                }

                Student profile:
                - Name: %s
                - Current band: %s
                - Study goal: %s

                Course context:
                - Course: %s
                - Minimum recommended current band: %s
                - Target band: %s
                - Target outcome: %s
                - Learning path: %s
                - Learning path stage: %s
                - Recommended next course slug: %s

                Module context:
                - Module title: %s
                - Module objective: %s
                - Target vocabulary list: %s

                Assessment context:
                - Assessment: %s
                - Type: %s
                - Skill: %s
                - AI mode: %s
                - Passing score: %s
                - Max score: %s
                - Instructions: %s
                - Objective answer key if available: %s
                - Speaking audio analysis state: %s
                - Skill-aware submission guidance: %s
                - Skill-specific scoring policy: %s

                Rubric:
                %s

                Student submission:
                %s
                """.formatted(
                student.getFullName(),
                safe(student.getCurrentBand()),
                safe(student.getStudyGoal()),
                course.getTitle(),
                safe(course.getRecommendedCurrentBandMin()),
                safe(course.getTargetBand()),
                safe(course.getTargetOutcome()),
                safe(course.getLearningPathName()),
                safe(course.getLearningPathOrder()),
                safe(course.getRecommendedNextCourseSlug()),
                assessment.getModule() == null ? "Not provided" : safe(assessment.getModule().getTitle()),
                assessment.getModule() == null ? "Not provided" : safe(assessment.getModule().getDescription()),
                safe(targetVocabulary),
                assessment.getTitle(),
                assessment.getType(),
                assessment.getSkill(),
                assessment.getAiEvaluationMode(),
                safe(assessment.getPassingScore()),
                safe(IeltsBandScale.resolveScoreCap(assessment)),
                safe(assessment.getInstructions()),
                safe(assessment.getObjectiveAnswerKey()),
                safe(speakingAudioState),
                safe(submissionGuidance),
                safe(skillEvaluationPolicy),
                criteriaText,
                safe(submittedContent)
        );
    }

    private String skillEvaluationPolicy(CourseAssessment assessment, boolean hasAnalyzableAudio) {
        AssessmentSkill skill = assessment.getSkill();
        if (skill == AssessmentSkill.LISTENING) {
            return "Listening is scored by stored answer key only. Do not invent a band or score. Use AI feedback only to explain wrong answers, weak sections, distractors, missed keywords, and review priorities.";
        }
        if (skill == AssessmentSkill.READING) {
            return "Reading is scored by stored answer key only. Do not invent a band or score. Use AI feedback only to explain wrong answers, passage evidence, weak question types, and review priorities.";
        }
        if (skill == AssessmentSkill.WRITING) {
            String taskType = assessment.getRubric() == null ? "" : safe(assessment.getRubric().getTaskType());
            return "Writing must be evaluated only with the linked IELTS Writing rubric. Task type: " + taskType + ". Do not create extra criteria outside the rubric.";
        }
        if (skill == AssessmentSkill.SPEAKING) {
            if (hasAnalyzableAudio) {
                return "Speaking audio is attached. Use audio as primary evidence for pronunciation, fluency, pace, pauses, stress, intonation, clarity, and delivery. Transcript is secondary context.";
            }
            return "No analyzable speaking audio is attached. Do not score Pronunciation or claim detailed pronunciation errors. You may only comment on content, grammar, vocabulary, and coherence visible in the provided text or metadata.";
        }
        if (skill == AssessmentSkill.MIXED) {
            return "Mixed assessment is for reflection and review planning only. Do not apply a Writing rubric unless the assessment is explicitly Writing.";
        }
        return "Use only the assessment instructions and linked rubric when available.";
    }

    private AiEvaluationResult normalizeEvaluationResult(AiEvaluationResult aiResult, CourseAssessment assessment) {
        if (aiResult == null) {
            return null;
        }

        BigDecimal normalizedScore = normalizeEstimatedScore(aiResult.getEstimatedScore(), assessment);
        aiResult.setEstimatedScore(normalizedScore);
        aiResult.setFeedbackJson(normalizeFeedbackJson(aiResult.getFeedbackJson(), assessment, normalizedScore));
        return aiResult;
    }

    private BigDecimal normalizeEstimatedScore(BigDecimal score, CourseAssessment assessment) {
        if (assessment.getAiEvaluationMode() == AiEvaluationMode.EXPLAIN_ONLY) {
            return null;
        }
        if (score == null) {
            return null;
        }
        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        score = IeltsBandScale.clampBandScore(score, assessment);
        return score;
    }

    private String normalizeFeedbackJson(String feedbackJson, CourseAssessment assessment, BigDecimal normalizedScore) {
        try {
            ObjectNode root = feedbackJson == null || feedbackJson.isBlank()
                    ? objectMapper.createObjectNode()
                    : (ObjectNode) objectMapper.readTree(feedbackJson);

            if (normalizedScore == null) {
                root.putNull("estimatedScore");
            } else {
                root.put("estimatedScore", normalizedScore);
            }

            if (assessment.getAiEvaluationMode() != AiEvaluationMode.ESTIMATED_BAND) {
                root.put("estimatedBand", "");
            } else if (!root.has("estimatedBand") || root.path("estimatedBand").isNull()) {
                root.put("estimatedBand", "");
            }

            if (assessment.getAiEvaluationMode() == AiEvaluationMode.EXPLAIN_ONLY) {
                appendUniqueText(root, "suggestions", "Ưu tiên xem đây là phản hồi phân tích lỗi và định hướng ôn tập, không phải điểm số chính thức.");
            }

            if (!root.has("disclaimer") || root.path("disclaimer").asText().isBlank()) {
                root.put("disclaimer", "Đây là phản hồi hỗ trợ học tập, không phải điểm IELTS chính thức.");
            }

            return objectMapper.writeValueAsString(root);
        } catch (Exception ignored) {
            return feedbackJson;
        }
    }

    private String buildSubmittedContent(AssessmentSubmissionRequest request, AssessmentSkill skill, boolean hasAnalyzableAudio) {
        String submittedText = request.getSubmittedText();
        String objectiveAnswers = request.getObjectiveAnswersJson();
        String audioUrl = request.getSubmittedAudioUrl();

        StringBuilder builder = new StringBuilder();
        if (hasText(submittedText)) {
            builder.append(skill == AssessmentSkill.SPEAKING ? "Transcript / spoken response:\n" : "Text response / notes:\n")
                    .append(submittedText.trim())
                    .append("\n\n");
        }
        if (hasText(objectiveAnswers)) {
            builder.append(skill == AssessmentSkill.LISTENING || skill == AssessmentSkill.READING ? "Answers and error notes:\n" : "Objective answers / structured notes:\n")
                    .append(formatObjectiveAnswers(objectiveAnswers))
                    .append("\n\n");
        }
        if (hasText(audioUrl)) {
            builder.append(hasAnalyzableAudio
                            ? "Attached audio file source URL (audio bytes are included separately in this request):\n"
                            : "Audio reference URL (not directly analyzed by the current AI pipeline):\n")
                    .append(audioUrl.trim())
                    .append("\n\n");
        }
        if (skill == AssessmentSkill.SPEAKING && hasAnalyzableAudio) {
            builder.append("Audio-native instruction:\n")
                    .append("Use the attached audio bytes as primary evidence for pronunciation, fluency, pauses, pace, stress, intonation, and delivery. Use transcript/metadata only as secondary context.\n\n");
        }
        return builder.toString().trim();
    }

    private String formatObjectiveAnswers(String objectiveAnswers) {
        String raw = objectiveAnswers == null ? "" : objectiveAnswers.trim();
        if (raw.isBlank()) {
            return raw;
        }

        try {
            JsonNode root = objectMapper.readTree(raw);
            if (!root.isObject()) {
                return raw;
            }

            StringBuilder builder = new StringBuilder();
            JsonNode responses = root.path("responses");
            if (responses.isArray()) {
                for (JsonNode response : responses) {
                    String questionNumber = response.path("questionNumber").asText("");
                    String answerType = response.path("answerType").asText("");
                    String answer = response.path("answer").asText("");
                    String notes = response.path("notes").asText("");
                    if (questionNumber.isBlank() && answer.isBlank() && notes.isBlank()) {
                        continue;
                    }
                    builder.append("- Question ").append(questionNumber.isBlank() ? "?" : questionNumber);
                    if (!answerType.isBlank()) {
                        builder.append(" [").append(answerType).append("]");
                    }
                    if (!answer.isBlank()) {
                        builder.append(": ").append(answer);
                    }
                    if (!notes.isBlank()) {
                        builder.append("\n  Notes: ").append(notes);
                    }
                    builder.append("\n");
                }
            }

            JsonNode sectionNotes = root.path("sectionNotes");
            if (sectionNotes.isObject()) {
                sectionNotes.fields().forEachRemaining((entry) -> {
                    String value = entry.getValue() == null ? "" : entry.getValue().asText("");
                    if (!value.isBlank()) {
                        if (builder.length() > 0) {
                            builder.append("\n");
                        }
                        builder.append("Notes for ").append(entry.getKey()).append(":\n").append(value.trim()).append("\n");
                    }
                });
            }

            String overallNotes = root.path("overallNotes").asText("");
            if (!overallNotes.isBlank()) {
                if (builder.length() > 0) {
                    builder.append("\n");
                }
                builder.append("Overall notes:\n").append(overallNotes);
            }

            return builder.length() > 0 ? builder.toString().trim() : raw;
        } catch (Exception ignored) {
            return raw;
        }
    }

    private String skillSubmissionGuidance(AssessmentSkill skill) {
        if (skill == null) {
            return "Use the assessment instructions to interpret the learner submission.";
        }
        return switch (skill) {
            case LISTENING -> "The learner may submit an IELTS-style answer sheet for 40 listening questions plus section notes. Explain likely listening traps, distractors, missed keywords, and what to review.";
            case READING -> "The learner may submit an IELTS-style answer sheet for 40 reading questions plus passage notes. Explain likely reading traps, evidence use, time-management issues, and what to review.";
            case SPEAKING -> "For speaking, actual attached audio is primary evidence for pronunciation, fluency, pacing, pauses, stress, intonation, and delivery. Transcript and metadata are secondary context for content and task coverage.";
            case WRITING -> "The learner submits an essay or paragraph. Evaluate it as writing according to the linked writing rubric.";
            case VOCABULARY -> "The learner submits sentences using target vocabulary. Check meaning accuracy, collocation, sentence quality, and topic relevance.";
            default -> "The learner may submit a mixed practice-test reflection, answers, or error log. Identify weak skills and recommend review steps.";
        };
    }

    private AiEvaluationResult applySpeakingEvidenceGuard(AiEvaluationResult aiResult, CourseAssessment assessment, AssessmentSubmissionRequest request) {
        if (aiResult == null || assessment.getSkill() != AssessmentSkill.SPEAKING) {
            return aiResult;
        }
        if (aiResult.isAudioInputAnalyzed()) {
            return aiResult;
        }

        String submittedText = request.getSubmittedText();
        if (submittedText == null || submittedText.isBlank()) {
            return rewriteSpeakingEvidenceFeedback(aiResult, "Không có nội dung bài nói đủ tin cậy để chấm. Hãy ghi âm và trả lời đầy đủ từng phần của bài Speaking.");
        }

        if (isAudioReferenceOnlySpeakingSubmission(request)) {
            return rewriteSpeakingEvidenceFeedback(
                    aiResult,
                    "Hệ thống hiện mới nhận được đường dẫn hoặc thông tin bản ghi của bài Speaking, chưa phân tích trực tiếp âm thanh. Vì vậy chưa thể chấm chính xác phát âm, từ bạn thật sự nói hoặc các từ bị nghe nhầm khi phát âm chưa rõ."
            );
        }

        int durationSeconds = extractInt(submittedText, SPEAKING_DURATION_PATTERN);
        boolean voiceSignalDetected = submittedText.toLowerCase(Locale.ROOT).contains("voice signal detected: yes");
        boolean insufficientEvidence = !voiceSignalDetected || durationSeconds < 5;

        if (!insufficientEvidence) {
            return aiResult;
        }

        return rewriteSpeakingEvidenceFeedback(aiResult, "Bài nói hiện chưa có đủ bằng chứng để chấm. Hệ thống không ghi nhận được phần nói rõ ràng hoặc thời lượng nói quá ngắn.");
    }

    private boolean isAudioReferenceOnlySpeakingSubmission(AssessmentSubmissionRequest request) {
        String submittedText = request.getSubmittedText();
        if (submittedText == null || submittedText.isBlank()) {
            return false;
        }
        String normalized = submittedText.toLowerCase(Locale.ROOT);
        return hasText(request.getSubmittedAudioUrl())
                && normalized.contains("speaking mock test:")
                && normalized.contains("part prompts shown to the learner:");
    }

    private AiEvaluationResult rewriteSpeakingEvidenceFeedback(AiEvaluationResult aiResult, String evidenceMessage) {
        try {
            ObjectNode root = aiResult.getFeedbackJson() == null || aiResult.getFeedbackJson().isBlank()
                    ? objectMapper.createObjectNode()
                    : (ObjectNode) objectMapper.readTree(aiResult.getFeedbackJson());

            root.putNull("estimatedScore");
            root.put("estimatedBand", "");

            ArrayNode criteria = objectMapper.createArrayNode();
            criteria.add(createCriterionNode("Fluency and Coherence", evidenceMessage));
            criteria.add(createCriterionNode("Lexical Resource", evidenceMessage));
            criteria.add(createCriterionNode("Grammatical Range and Accuracy", evidenceMessage));
            criteria.add(createCriterionNode("Pronunciation", "Không có đủ dữ liệu nói rõ ràng để đánh giá phát âm, nhịp điệu và độ trôi chảy."));
            root.set("criteria", criteria);

            root.set("strengths", objectMapper.createArrayNode());
            root.set("weaknesses", objectMapper.createArrayNode().add(evidenceMessage));
            root.set("suggestions", objectMapper.createArrayNode()
                    .add("Làm lại bài nói và trả lời thật từng câu hỏi thay vì bỏ trống hoặc để bản ghi quá ngắn.")
                    .add("Với Part 1, hãy trả lời trực diện. Với Part 2, nói liên tục theo cue card. Với Part 3, phát triển ý với lý do và ví dụ."));
            root.set("recommendedReview", objectMapper.createArrayNode()
                    .add("Kiểm tra lại micro, môi trường thu âm và làm lại bài Speaking từ đầu."));
            root.set("partFeedback", objectMapper.createArrayNode()
                    .add(createPartFeedbackNode("part_1", "Part 1", "Chưa có đủ câu trả lời để đánh giá phản xạ và độ tự nhiên ở Part 1.", evidenceMessage))
                    .add(createPartFeedbackNode("part_2", "Part 2", "Chưa có đủ bằng chứng để đánh giá khả năng nói liên tục theo cue card.", evidenceMessage))
                    .add(createPartFeedbackNode("part_3", "Part 3", "Chưa có đủ bằng chứng để đánh giá khả năng phản biện và phát triển ý ở Part 3.", evidenceMessage)));

            aiResult.setEstimatedScore(null);
            aiResult.setFeedbackJson(objectMapper.writeValueAsString(root));
            return aiResult;
        } catch (Exception ignored) {
            aiResult.setEstimatedScore(null);
            return aiResult;
        }
    }

    private ObjectNode createCriterionNode(String name, String feedback) {
        return createCriterionNode(name, 0, feedback);
    }

    private ObjectNode createCriterionNode(String name, int score, String feedback) {
        ObjectNode criterion = objectMapper.createObjectNode();
        criterion.put("name", name);
        criterion.put("score", score);
        criterion.put("feedback", feedback);
        return criterion;
    }

    private ObjectNode createPartFeedbackNode(String partKey, String partLabel, String summary, String weakness) {
        ObjectNode partNode = objectMapper.createObjectNode();
        partNode.put("partKey", partKey);
        partNode.put("partLabel", partLabel);
        partNode.put("summary", summary);
        partNode.set("strengths", objectMapper.createArrayNode());
        partNode.set("weaknesses", objectMapper.createArrayNode().add(weakness));
        partNode.set("suggestions", objectMapper.createArrayNode()
                .add("Làm lại " + partLabel + " với câu trả lời đầy đủ hơn, rõ hơn và có phần triển khai ý."));
        return partNode;
    }

    private int extractInt(String source, Pattern pattern) {
        if (source == null || source.isBlank()) {
            return 0;
        }
        var matcher = pattern.matcher(source);
        if (!matcher.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private AiEvaluationResult applyVocabularyRelevanceGuard(AiEvaluationResult aiResult, CourseAssessment assessment, String submittedText, String targetVocabulary) {
        if (aiResult == null || assessment.getSkill() != AssessmentSkill.VOCABULARY) {
            return aiResult;
        }
        if (submittedText == null || submittedText.isBlank() || targetVocabulary == null || targetVocabulary.equals("Not provided")) {
            return aiResult;
        }

        List<String> targetTerms = List.of(targetVocabulary.split(",\\s*")).stream()
                .map(String::trim)
                .filter(term -> !term.isBlank())
                .toList();
        long overlapCount = targetTerms.stream()
                .filter(term -> containsWholePhrase(submittedText, term))
                .count();

        if (overlapCount > 0) {
            return aiResult;
        }

        BigDecimal scoreCap = IeltsBandScale.resolveScoreCap(assessment);
        BigDecimal cap = scoreCap == null
                ? BigDecimal.valueOf(3)
                : scoreCap.multiply(VOCABULARY_OFF_TOPIC_CAP_RATIO);
        if (IeltsBandScale.usesBandScale(assessment)) {
            cap = IeltsBandScale.normalizeBand(cap);
        }
        if (aiResult.getEstimatedScore() == null || aiResult.getEstimatedScore().compareTo(cap) > 0) {
            aiResult.setEstimatedScore(cap);
        }
        aiResult.setFeedbackJson(rewriteVocabularyGuardFeedback(aiResult.getFeedbackJson(), cap, assessment, targetTerms));
        return aiResult;
    }

    private String rewriteVocabularyGuardFeedback(String feedbackJson, BigDecimal cappedScore, CourseAssessment assessment, List<String> targetTerms) {
        String targetPreview = targetTerms.stream().limit(6).reduce((left, right) -> left + ", " + right).orElse("bộ từ vựng mục tiêu");
        String missingVocabularyMessage = "Bài làm chưa sử dụng đúng hoặc đủ bộ từ vựng mục tiêu của module, nên Meaning Accuracy và Topic Relevance không thể được chấm cao.";
        String reviewMessage = "Hãy viết lại bài và dùng tự nhiên các từ/cụm từ mục tiêu trong ngữ cảnh phù hợp, ví dụ: " + targetPreview + ".";

        try {
            ObjectNode root = feedbackJson == null || feedbackJson.isBlank()
                    ? objectMapper.createObjectNode()
                    : (ObjectNode) objectMapper.readTree(feedbackJson);
            root.put("estimatedScore", cappedScore);
            if (!root.has("estimatedBand")) {
                root.put("estimatedBand", "");
            }

            ArrayNode criteria = ensureArray(root, "criteria");
            upsertCriterion(criteria, "Meaning Accuracy", BigDecimal.ZERO, missingVocabularyMessage);
            upsertCriterion(criteria, "Topic Relevance", BigDecimal.ZERO, "Bài làm còn lệch khỏi chủ đề của module và chưa bám vào danh sách từ vựng mục tiêu.");

            appendUniqueText(root, "weaknesses", missingVocabularyMessage);
            appendUniqueText(root, "suggestions", reviewMessage);
            appendUniqueText(root, "recommendedReview", "Ôn lại bài từ vựng của module rồi làm lại bài kiểm tra này.");

            return objectMapper.writeValueAsString(root);
        } catch (Exception ignored) {
            return """
                    {"estimatedScore":%s,"estimatedBand":"","criteria":[{"name":"Meaning Accuracy","score":0,"feedback":"%s"},{"name":"Topic Relevance","score":0,"feedback":"Bài làm còn lệch khỏi chủ đề của module và chưa bám vào danh sách từ vựng mục tiêu."}],"strengths":[],"weaknesses":["%s"],"suggestions":["%s"],"recommendedReview":["Ôn lại bài từ vựng của module rồi làm lại bài kiểm tra này."],"correctedExamples":[]}
                    """.formatted(cappedScore.toPlainString(), escapeJson(missingVocabularyMessage), escapeJson(missingVocabularyMessage), escapeJson(reviewMessage));
        }
    }

    private ArrayNode ensureArray(ObjectNode root, String fieldName) {
        JsonNode node = root.get(fieldName);
        if (node instanceof ArrayNode arrayNode) {
            return arrayNode;
        }
        ArrayNode arrayNode = objectMapper.createArrayNode();
        root.set(fieldName, arrayNode);
        return arrayNode;
    }

    private void upsertCriterion(ArrayNode criteria, String name, BigDecimal score, String feedback) {
        for (JsonNode criterionNode : criteria) {
            if (!(criterionNode instanceof ObjectNode criterion)) {
                continue;
            }
            if (name.equalsIgnoreCase(criterion.path("name").asText())) {
                criterion.put("score", score);
                criterion.put("feedback", feedback);
                return;
            }
        }

        ObjectNode criterion = objectMapper.createObjectNode();
        criterion.put("name", name);
        criterion.put("score", score);
        criterion.put("feedback", feedback);
        criteria.add(criterion);
    }

    private void appendUniqueText(ObjectNode root, String fieldName, String value) {
        ArrayNode array = ensureArray(root, fieldName);
        boolean exists = false;
        for (JsonNode item : array) {
            if (value.equalsIgnoreCase(item.asText())) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            array.add(value);
        }
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private boolean containsWholePhrase(String text, String phrase) {
        String normalizedText = " " + text.toLowerCase(Locale.ROOT) + " ";
        String normalizedPhrase = " " + phrase.toLowerCase(Locale.ROOT) + " ";
        return normalizedText.contains(normalizedPhrase);
    }

    private boolean hasSubmissionContent(AssessmentSubmissionRequest request) {
        return request != null && (
                hasText(request.getSubmittedText())
                        || hasText(request.getObjectiveAnswersJson())
                        || hasText(request.getSubmittedAudioUrl())
        );
    }

    private boolean isInsufficientWritingSubmission(
            CourseAssessment assessment,
            AssessmentSubmissionRequest request
    ) {
        if (assessment.getSkill() != AssessmentSkill.WRITING || request == null) {
            return false;
        }
        String submittedText = request.getSubmittedText();
        if (submittedText == null || submittedText.isBlank()) {
            return true;
        }
        String learnerText = submittedText.replaceAll("(?m)^\\s*\\[[^]]+]\\s*$", " ").trim();
        long meaningfulWords = java.util.Arrays.stream(learnerText.split("\\s+"))
                .filter(word -> word.codePoints().anyMatch(Character::isLetter))
                .count();
        return meaningfulWords < 5;
    }

    private AiEvaluationResult buildInsufficientWritingResult(CourseAssessment assessment) {
        BigDecimal score = assessment.getAiEvaluationMode() == AiEvaluationMode.EXPLAIN_ONLY
                ? null
                : BigDecimal.ZERO;
        ObjectNode feedback = objectMapper.createObjectNode();
        if (score == null) {
            feedback.putNull("estimatedScore");
        } else {
            feedback.put("estimatedScore", score);
        }
        feedback.put("estimatedBand", score == null ? "" : "0");
        feedback.put("isOfficialScore", false);
        feedback.put("summary", "Bài làm quá ngắn và chưa có đủ nội dung để thể hiện năng lực Writing.");
        feedback.set("criteria", objectMapper.createArrayNode()
                .add(createCriterionNode("Task Response", 0, "Bài làm chưa phát triển câu trả lời cho yêu cầu của đề."))
                .add(createCriterionNode("Coherence and Cohesion", 0, "Chưa có đủ câu và ý để đánh giá bố cục hoặc liên kết."))
                .add(createCriterionNode("Lexical Resource", 0, "Chưa có đủ từ vựng để đánh giá mức độ sử dụng ngôn ngữ."))
                .add(createCriterionNode("Grammatical Range and Accuracy", 0, "Chưa có đủ cấu trúc câu để đánh giá ngữ pháp.")));
        feedback.set("strengths", objectMapper.createArrayNode());
        feedback.set("weaknesses", objectMapper.createArrayNode()
                .add("Bài làm chưa cung cấp đủ bằng chứng để chấm theo tiêu chí Writing."));
        feedback.set("suggestions", objectMapper.createArrayNode()
                .add("Viết lại thành câu hoàn chỉnh, trả lời đúng yêu cầu và phát triển ý bằng lý do hoặc ví dụ."));
        feedback.set("recommendedReview", objectMapper.createArrayNode()
                .add("Xem lại yêu cầu đề và số từ tối thiểu trước khi làm lại."));

        return AiEvaluationResult.builder()
                .estimatedScore(score)
                .feedbackJson(feedback.toString())
                .provider("SYSTEM")
                .model("WRITING_EVIDENCE_GUARD")
                .rawResponse("Writing submission contained fewer than five meaningful words.")
                .build();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String safe(Object value) {
        return value == null ? "Not provided" : value.toString();
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private String extractUiConfigJson(String instructions) {
        if (instructions == null || instructions.isBlank() || !instructions.contains(UI_CONFIG_MARKER)) {
            return null;
        }
        return instructions.substring(instructions.indexOf(UI_CONFIG_MARKER) + UI_CONFIG_MARKER.length()).trim();
    }

    private String sanitizeUiConfigJson(String uiConfigJson) {
        if (uiConfigJson == null || uiConfigJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(uiConfigJson);
            if (root.isObject()) {
                ((ObjectNode) root).remove("answerKey");
            }
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException exception) {
            throw new RuntimeException("Cấu hình giao diện bài thi không hợp lệ.");
        }
    }

    private String extractDisplayInstructions(String instructions) {
        if (instructions == null || instructions.isBlank()) {
            return instructions;
        }
        int markerIndex = instructions.indexOf(UI_CONFIG_MARKER);
        if (markerIndex < 0) {
            return instructions;
        }
        return instructions.substring(0, markerIndex).trim();
    }

    private CourseAssessmentResponse toResponse(CourseAssessment assessment, User student) {
        List<AssessmentSubmission> submissionHistory = assessment.getProgressKey() == null
                || assessment.getProgressKey().isBlank()
                ? submissionRepository.findTop2ByAssessmentAndStudentOrderBySubmittedAtDesc(assessment, student)
                : submissionRepository.findTop2ByAssessmentProgressKeyAndStudentOrderBySubmittedAtDesc(
                        assessment.getProgressKey(),
                        student
                );
        List<AiAssessmentSubmissionResponse> recentSubmissions = submissionHistory
                .stream()
                .map(this::toSubmissionResponse)
                .toList();
        AiAssessmentSubmissionResponse latestSubmission = recentSubmissions.isEmpty() ? null : recentSubmissions.get(0);
        AiAssessmentSubmissionResponse previousSubmission = recentSubmissions.size() > 1 ? recentSubmissions.get(1) : null;
        String rawInstructions = assessment.getInstructions();
        return CourseAssessmentResponse.builder()
                .id(assessment.getId())
                .courseId(assessment.getOnlineCourse().getId())
                .moduleId(assessment.getModule() == null ? null : assessment.getModule().getId())
                .assessmentBankItemId(assessment.getAssessmentBankItem() == null ? null : assessment.getAssessmentBankItem().getId())
                .moduleTitle(assessment.getModule() == null ? null : assessment.getModule().getTitle())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .type(assessment.getType())
                .skill(assessment.getSkill())
                .aiEvaluationMode(assessment.getAiEvaluationMode())
                .instructions(extractDisplayInstructions(rawInstructions))
                .objectiveAnswerKey(null)
                .uiConfigJson(sanitizeUiConfigJson(
                        assessment.getUiConfigJson() == null || assessment.getUiConfigJson().isBlank()
                                ? extractUiConfigJson(rawInstructions)
                                : assessment.getUiConfigJson()
                ))
                .passingScore(assessment.getPassingScore())
                .maxScore(IeltsBandScale.resolveScoreCap(assessment))
                .resolvedPassingThreshold(passingThresholdResolver.resolve(assessment))
                .passingThresholdLabel(passingThresholdResolver.buildDisplayLabel(assessment))
                .timeLimitMinutes(assessment.getTimeLimitMinutes())
                .displayOrder(assessment.getDisplayOrder())
                .active(assessment.isActive())
                .rubric(toRubricResponse(assessment.getRubric()))
                .latestSubmission(latestSubmission)
                .previousSubmission(previousSubmission)
                .build();
    }

    private void normalizeAssessmentRubricCompatibility(CourseAssessment assessment) {
        if (assessment == null) {
            return;
        }
        if (assessment.getSkill() == AssessmentSkill.LISTENING
                || assessment.getSkill() == AssessmentSkill.READING) {
            // Older snapshots could retain a Writing rubric after switching to
            // an objective bank item. Objective scoring never uses a rubric.
            assessment.setRubric(null);
        }
    }

    private String extractTargetVocabulary(OnlineCourseModule module) {
        if (module == null || module.getLessons() == null) {
            return "Not provided";
        }
        return module.getLessons().stream()
                .map(OnlineLesson::getContentText)
                .filter(content -> content != null && content.contains("### "))
                .flatMap(content -> VOCABULARY_HEADING.matcher(content).results().map(match -> cleanMarkdown(match.group(1))))
                .filter(term -> !term.isBlank())
                .distinct()
                .limit(20)
                .reduce((left, right) -> left + ", " + right)
                .orElse("Not provided");
    }

    private String cleanMarkdown(String value) {
        return value == null ? "" : value.replace("**", "").replaceAll("^['\"]|['\"]$", "").trim();
    }

    private AssessmentRubricResponse toRubricResponse(AssessmentRubric rubric) {
        if (rubric == null) return null;
        return AssessmentRubricResponse.builder()
                .id(rubric.getId())
                .name(rubric.getName())
                .examType(rubric.getExamType())
                .skill(rubric.getSkill())
                .taskType(rubric.getTaskType())
                .scoringScale(rubric.getScoringScale())
                .description(rubric.getDescription())
                .active(rubric.isActive())
                .criteria(rubric.getCriteria().stream()
                        .sorted(Comparator.comparing(RubricCriterion::getDisplayOrder).thenComparing(RubricCriterion::getId))
                        .map(criterion -> RubricCriterionResponse.builder()
                                .id(criterion.getId())
                                .name(criterion.getName())
                                .weight(criterion.getWeight())
                                .description(criterion.getDescription())
                                .bandDescriptors(criterion.getBandDescriptors())
                                .displayOrder(criterion.getDisplayOrder())
                                .build())
                        .toList())
                .build();
    }

    private AiAssessmentSubmissionResponse toSubmissionResponse(AssessmentSubmission submission) {
        return AiAssessmentSubmissionResponse.builder()
                .id(submission.getId())
                .assessmentId(submission.getAssessment().getId())
                .assessmentTitle(submission.getAssessment().getTitle())
                .submittedText(submission.getSubmittedText())
                .submittedAudioUrl(submission.getSubmittedAudioUrl())
                .objectiveAnswersJson(submission.getObjectiveAnswersJson())
                .aiScore(submission.getAiScore())
                .aiFeedbackJson(submission.getAiFeedbackJson())
                .aiPromptSnapshot(submission.getAiPromptSnapshot())
                .status(submission.getStatus())
                .aiProvider(submission.getAiProvider())
                .aiModel(submission.getAiModel())
                .submittedAt(submission.getSubmittedAt())
                .build();
    }
}
