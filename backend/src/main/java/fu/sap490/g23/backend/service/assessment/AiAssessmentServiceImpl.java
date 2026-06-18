package fu.sap490.g23.backend.service.assessment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fu.sap490.g23.backend.dto.request.assessment.AssessmentSubmissionRequest;
import fu.sap490.g23.backend.dto.response.assessment.*;
import fu.sap490.g23.backend.entity.User;
import fu.sap490.g23.backend.entity.assessment.*;
import fu.sap490.g23.backend.entity.course.CourseModule;
import fu.sap490.g23.backend.entity.course.Lesson;
import fu.sap490.g23.backend.entity.course.OnlineCourse;
import fu.sap490.g23.backend.entity.course.PackageEnrollment;
import fu.sap490.g23.backend.repository.UserRepository;
import fu.sap490.g23.backend.service.ai.AiEvaluationClient;
import fu.sap490.g23.backend.service.ai.AiEvaluationResult;
import fu.sap490.g23.backend.service.course.CourseProgressService;
import fu.sap490.g23.backend.service.course.CourseProgressionGuard;
import fu.sap490.g23.backend.repository.assessment.AssessmentSubmissionRepository;
import fu.sap490.g23.backend.repository.assessment.CourseAssessmentRepository;
import fu.sap490.g23.backend.repository.course.OnlineCourseRepository;
import fu.sap490.g23.backend.repository.course.PackageEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final PackageEnrollmentRepository enrollmentRepository;
    private final UserRepository userRepository;
    private final AiEvaluationClient aiEvaluationClient;
    private final AssessmentAudioStorageService assessmentAudioStorageService;
    private final CourseProgressService courseProgressService;
    private final CourseProgressionGuard courseProgressionGuard;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional(readOnly = true)
    public List<CourseAssessmentResponse> getCourseAssessments(Long courseId, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail).orElseThrow(() -> new RuntimeException("Student not found"));
        OnlineCourse course = onlineCourseRepository.findById(courseId).orElseThrow(() -> new RuntimeException("Course not found"));
        ensureEnrolled(student, course);
        return courseAssessmentRepository.findByOnlineCourseAndActiveTrueOrderByDisplayOrderAscIdAsc(course)
                .stream()
                .map(assessment -> toResponse(assessment, student))
                .toList();
    }

    @Override
    public AiAssessmentSubmissionResponse submitAssessment(Long assessmentId, AssessmentSubmissionRequest request, String studentEmail) {
        User student = userRepository.findByEmail(studentEmail).orElseThrow(() -> new RuntimeException("Student not found"));
        CourseAssessment assessment = courseAssessmentRepository.findById(assessmentId).orElseThrow(() -> new RuntimeException("Assessment not found"));
        ensureEnrolled(student, assessment.getOnlineCourse());
        courseProgressionGuard.ensureAssessmentCanBeSubmitted(student, assessment);

        if (assessment.getAiEvaluationMode() == AiEvaluationMode.NONE) {
            throw new RuntimeException("This assessment does not enable AI evaluation");
        }
        if (!hasSubmissionContent(request)) {
            throw new RuntimeException("Student submission is empty. Provide text, transcript, answers, or an audio URL before requesting AI feedback.");
        }

        var speakingAudio = resolveSpeakingAudio(assessment, request);
        String prompt = buildRubricPrompt(assessment, request, student, speakingAudio.isPresent());
        String submittedText = firstNonBlank(request.getSubmittedText(), request.getObjectiveAnswersJson());
        String targetVocabulary = extractTargetVocabulary(assessment.getModule());
        AiEvaluationResult aiResult;
        if (usesObjectiveAnswerKey(assessment, request)) {
            aiResult = evaluateObjectiveAssessment(assessment, request);
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
                .aiScore(score)
                .aiFeedbackJson(aiResult.getFeedbackJson())
                .aiPromptSnapshot(prompt)
                .aiProvider(aiResult.getProvider())
                .aiModel(aiResult.getModel())
                .aiRawResponse(aiResult.getRawResponse())
                .status(status)
                .build();

        AssessmentSubmission savedSubmission = submissionRepository.save(submission);
        enrollmentRepository.findByStudentAndLearningPackage(student, assessment.getOnlineCourse().getLearningPackage())
                .ifPresent(enrollment -> courseProgressService.refreshEnrollmentProgress(enrollment, assessment.getOnlineCourse(), student));
        return toSubmissionResponse(savedSubmission);
    }

    private void ensureEnrolled(User student, OnlineCourse course) {
        PackageEnrollment enrollment = enrollmentRepository.findByStudentAndLearningPackage(student, course.getLearningPackage())
                .orElseThrow(() -> new RuntimeException("Student is not enrolled in this online course"));
        if (enrollment.getStatus() != null && enrollment.getStatus().name().equals("CANCELLED")) {
            throw new RuntimeException("Enrollment is not active");
        }
    }

    private SubmissionStatus resolveSubmissionStatus(BigDecimal score, CourseAssessment assessment) {
        if (assessment.getAiEvaluationMode() == AiEvaluationMode.EXPLAIN_ONLY) {
            return SubmissionStatus.AI_EVALUATED;
        }
        if (score == null) {
            return SubmissionStatus.AI_EVALUATED;
        }

        BigDecimal passingThreshold = assessment.getPassingScore();
        if (assessment.getMaxScore() != null) {
            BigDecimal minimumCourseraThreshold = assessment.getMaxScore().multiply(BigDecimal.valueOf(0.7));
            passingThreshold = passingThreshold == null
                    ? minimumCourseraThreshold
                    : passingThreshold.max(minimumCourseraThreshold);
        }

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
        if (assessment.getSkill() != AssessmentSkill.LISTENING && assessment.getSkill() != AssessmentSkill.READING) {
            return false;
        }
        return hasText(assessment.getObjectiveAnswerKey()) && hasText(request.getObjectiveAnswersJson());
    }

    private AiEvaluationResult evaluateObjectiveAssessment(CourseAssessment assessment, AssessmentSubmissionRequest request) {
        try {
            JsonNode answerKeyRoot = objectMapper.readTree(assessment.getObjectiveAnswerKey());
            JsonNode submissionRoot = objectMapper.readTree(request.getObjectiveAnswersJson());
            ObjectiveEvaluationSummary summary = scoreObjectiveAssessment(assessment, answerKeyRoot, submissionRoot);
            ObjectNode feedback = buildObjectiveFeedback(summary);
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

            String expected = resolveExpectedAnswer(answerKeyRoot, questionNumber);
            boolean correct = normalizeAnswer(studentAnswer).equals(normalizeAnswer(expected));
            summary.totalCount += 1;
            partSummary.totalCount += 1;
            if (correct) {
                summary.correctCount += 1;
                partSummary.correctCount += 1;
                partSummary.strengths.add("Câu " + questionNumber + " đúng.");
            } else {
                String feedback = "Câu " + questionNumber + ": đáp án đúng là " + fallbackText(expected) + ", bài làm của bạn là " + fallbackText(studentAnswer) + ".";
                partSummary.weaknesses.add(feedback);
            }
        }

        return summary;
    }

    private ObjectNode buildObjectiveFeedback(ObjectiveEvaluationSummary summary) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("estimatedScore", summary.correctCount);
        root.put("estimatedBand", "");

        ArrayNode criteria = objectMapper.createArrayNode();
        ArrayNode strengths = objectMapper.createArrayNode();
        ArrayNode weaknesses = objectMapper.createArrayNode();
        ArrayNode suggestions = objectMapper.createArrayNode();
        ArrayNode recommendedReview = objectMapper.createArrayNode();
        ArrayNode partFeedback = objectMapper.createArrayNode();
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
                partSummary.weaknesses.add("Phần này chưa có lỗi rõ rệt.");
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
        root.set("correctedExamples", correctedExamples);
        root.put("plagiarismRisk", "LOW");
        root.put("aiUsageRisk", "LOW");
        root.set("sourceSignals", sourceSignals.add("Đối chiếu trực tiếp từng câu trả lời với đáp án chuẩn được lưu trong đề."));
        originality.put("summary", "Bài làm là bài trả lời khách quan nên hệ thống đối chiếu trực tiếp với đáp án chuẩn thay vì suy đoán theo văn phong.");
        originality.put("plagiarismRisk", "LOW");
        originality.put("aiUsageRisk", "LOW");
        root.set("originalityAnalysis", originality);
        root.put("disclaimer", "This is AI-assisted feedback and not an official certification score.");
        return root;
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

    private String resolveExpectedAnswer(JsonNode answerKeyRoot, String questionNumber) {
        if (answerKeyRoot == null || !answerKeyRoot.isObject()) {
            return "";
        }
        JsonNode directNode = answerKeyRoot.path(questionNumber);
        if (directNode.isMissingNode() || directNode.isNull()) {
            return "";
        }
        if (directNode.isArray()) {
            List<String> values = new ArrayList<>();
            directNode.forEach(item -> values.add(item.asText("")));
            return String.join(", ", values);
        }
        return directNode.asText("");
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
                - Give specific evidence from the student's submission, not generic advice.
                - Suggestions and recommendedReview must connect back to the course/module learning path.
                - correctedExamples must explain errors clearly for English learners.
                - Add an originality analysis based only on the submission text itself. Do not claim web search or exact source matching.
                - If the submission looks memorized, copied, template-heavy, or likely AI-assisted, report that as a risk signal with reasons.
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
                - For speaking mock tests, if the submission includes part metadata, provide a separate partFeedback entry for Part 1, Part 2, and Part 3 whenever possible.
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
                  "disclaimer": "This is AI-assisted feedback and not an official certification score."
                }

                Student profile:
                - Name: %s
                - Current band: %s
                - Study goal: %s

                Course context:
                - Course: %s
                - Recommended current band: %s - %s
                - Target band: %s
                - Target outcome: %s
                - Learning path: %s
                - Learning path step: %s
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

                Rubric:
                %s

                Student submission:
                %s
                """.formatted(
                student.getFullName(),
                safe(student.getCurrentBand()),
                safe(student.getStudyGoal()),
                course.getLearningPackage().getTitle(),
                safe(course.getRecommendedCurrentBandMin()),
                safe(course.getRecommendedCurrentBandMax()),
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
                safe(assessment.getMaxScore()),
                safe(assessment.getInstructions()),
                safe(assessment.getObjectiveAnswerKey()),
                safe(speakingAudioState),
                safe(submissionGuidance),
                criteriaText,
                safe(submittedContent)
        );
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
        if (assessment.getMaxScore() != null && score.compareTo(assessment.getMaxScore()) > 0) {
            score = assessment.getMaxScore();
        }
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
                root.put("disclaimer", "This is AI-assisted feedback and not an official certification score.");
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
            return rewriteSpeakingEvidenceFeedback(aiResult, "Khong co noi dung bai noi du tin cay de cham. Hay ghi am va tra loi day du tung phan cua bai Speaking.");
        }

        if (isAudioReferenceOnlySpeakingSubmission(request)) {
            return rewriteSpeakingEvidenceFeedback(
                    aiResult,
                    "He thong hien moi nhan duoc URL ban ghi hoac metadata cua bai Speaking, chu chua phan tich truc tiep am thanh. Vi vay chua the cham chinh xac phat am, tu ban thuc su noi, hoac cac tu bi nhan nham khi phat am chua ro."
            );
        }

        int durationSeconds = extractInt(submittedText, SPEAKING_DURATION_PATTERN);
        boolean voiceSignalDetected = submittedText.toLowerCase(Locale.ROOT).contains("voice signal detected: yes");
        boolean insufficientEvidence = !voiceSignalDetected || durationSeconds < 5;

        if (!insufficientEvidence) {
            return aiResult;
        }

        return rewriteSpeakingEvidenceFeedback(aiResult, "Bai noi hien chua co du bang chung de cham. He thong khong ghi nhan duoc phan noi ro rang hoac thoi luong noi qua ngan.");
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
            criteria.add(createCriterionNode("Pronunciation", "Kh?ng c? ?? d? li?u n?i r? r?ng ?? ??nh gi? ph?t ?m, nh?p ?i?u v? ?? tr?i ch?y."));
            root.set("criteria", criteria);

            root.set("strengths", objectMapper.createArrayNode());
            root.set("weaknesses", objectMapper.createArrayNode().add(evidenceMessage));
            root.set("suggestions", objectMapper.createArrayNode()
                    .add("L?m l?i b?i n?i v? tr? l?i th?t t?ng c?u h?i thay v? b? tr?ng ho?c ?? b?n ghi qu? ng?n.")
                    .add("V?i Part 1, h?y tr? l?i tr?c di?n. V?i Part 2, n?i li?n t?c theo cue card. V?i Part 3, ph?t tri?n ? v?i l? do v? v? d?."));
            root.set("recommendedReview", objectMapper.createArrayNode()
                    .add("Ki?m tra l?i micro, m?i tr??ng thu ?m v? l?m l?i b?i Speaking t? ??u."));
            root.set("partFeedback", objectMapper.createArrayNode()
                    .add(createPartFeedbackNode("part_1", "Part 1", "Ch?a c? ?? c?u tr? l?i ?? ??nh gi? ph?n x? v? ?? t? nhi?n ? Part 1.", evidenceMessage))
                    .add(createPartFeedbackNode("part_2", "Part 2", "Ch?a c? ?? b?ng ch?ng ?? ??nh gi? kh? n?ng n?i li?n t?c theo cue card.", evidenceMessage))
                    .add(createPartFeedbackNode("part_3", "Part 3", "Ch?a c? ?? b?ng ch?ng ?? ??nh gi? kh? n?ng ph?n bi?n v? ph?t tri?n ? ? Part 3.", evidenceMessage)));

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
                .add("L?m l?i " + partLabel + " v?i c?u tr? l?i ??y ?? h?n, r? h?n v? c? ph?n tri?n khai ?."));
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

        BigDecimal cap = assessment.getMaxScore() == null
                ? BigDecimal.valueOf(3)
                : assessment.getMaxScore().multiply(VOCABULARY_OFF_TOPIC_CAP_RATIO);
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
        List<AiAssessmentSubmissionResponse> recentSubmissions = submissionRepository
                .findTop2ByAssessmentAndStudentOrderBySubmittedAtDesc(assessment, student)
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
                .moduleTitle(assessment.getModule() == null ? null : assessment.getModule().getTitle())
                .title(assessment.getTitle())
                .description(assessment.getDescription())
                .type(assessment.getType())
                .skill(assessment.getSkill())
                .aiEvaluationMode(assessment.getAiEvaluationMode())
                .instructions(extractDisplayInstructions(rawInstructions))
                .objectiveAnswerKey(assessment.getObjectiveAnswerKey())
                .uiConfigJson(extractUiConfigJson(rawInstructions))
                .passingScore(assessment.getPassingScore())
                .maxScore(assessment.getMaxScore())
                .timeLimitMinutes(assessment.getTimeLimitMinutes())
                .displayOrder(assessment.getDisplayOrder())
                .active(assessment.isActive())
                .rubric(toRubricResponse(assessment.getRubric()))
                .latestSubmission(latestSubmission)
                .previousSubmission(previousSubmission)
                .build();
    }

    private String extractTargetVocabulary(CourseModule module) {
        if (module == null || module.getLessons() == null) {
            return "Not provided";
        }
        return module.getLessons().stream()
                .map(Lesson::getContentText)
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
