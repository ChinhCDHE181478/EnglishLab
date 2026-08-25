package fu.sep490.g23.backend.service.assessment.impl;
import fu.sep490.g23.backend.service.assessment.IeltsBandScale;
import fu.sep490.g23.backend.service.assessment.PlacementTestService;
import fu.sep490.g23.backend.service.assessment.AssessmentAudioStorageService;
import fu.sep490.g23.backend.service.assessment.PlacementTestDefinitionService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import fu.sep490.g23.backend.dto.request.assessment.PlacementTestSubmissionRequest;
import fu.sep490.g23.backend.dto.response.assessment.PlacementTestAttemptResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import fu.sep490.g23.backend.entity.assessment.PlacementTestDefinition;
import fu.sep490.g23.backend.entity.curriculum.ContentBankItem;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.curriculum.ContentBankItemRepository;
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.ai.AiEvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlacementTestServiceImpl implements PlacementTestService {
    private static final String TEST_CODE = PlacementTestDefinitionService.TEST_CODE;
    private static final Pattern SPEAKING_METADATA_PATTERN = Pattern.compile("speaking mock test:|part prompts shown to the learner:|recording duration seconds:|voice signal detected:", Pattern.CASE_INSENSITIVE);
    private static final Set<String> WRITING_TASK_1_KEYWORDS = Set.of(
            "corn", "ethanol", "fuel", "process", "production", "produce", "diagram", "stages", "ferment", "fermentation", "liquid", "milling", "cook", "cooking", "purify", "purification"
    );
    private static final Set<String> WRITING_TASK_2_KEYWORDS = Set.of(
            "physical", "mental", "strength", "sport", "sports", "athlete", "athletes", "success", "training", "performance", "competition", "competitive"
    );
    private static final Set<String> SPEAKING_TOPIC_KEYWORDS = Set.of(
            "from", "live", "home", "hometown", "films", "film", "movie", "movies", "watch",
            "leisure", "activity", "activities", "work", "adults", "children", "parents",
            "generation", "generations", "free", "time", "enjoy"
    );

    private final UserRepository userRepository;
    private final PlacementTestAttemptRepository attemptRepository;
    private final AiEvaluationClient aiEvaluationClient;
    private final AssessmentAudioStorageService audioStorageService;
    private final PlacementTestDefinitionService definitionService;
    private final ContentBankItemRepository contentBankItemRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public Map<String, Object> getTest(String studentEmail) {
        User student = requireStudent(studentEmail);
        var definition = definitionService.getDefinition();
        if (!definition.isActive()) {
            throw new IllegalStateException("Bài đánh giá đầu vào hiện đang tạm dừng.");
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("testCode", TEST_CODE);
        response.put("title", definition.getTitle());
        response.put("description", definition.getDescription());
        response.put("examType", definition.getExamType());
        long attemptCount = attemptRepository.countByStudentAndTestCode(student, TEST_CODE);
        response.put("attemptCount", attemptCount);
        response.put("canRetake", true);
        Map<String, Object> sections = new LinkedHashMap<>();
        sections.put("listening", toPlainObject(withoutAnswerKey(definitionService.getConfig(definition, "listening"))));
        sections.put("reading", toPlainObject(withoutAnswerKey(definitionService.getConfig(definition, "reading"))));
        sections.put("writing", toPlainObject(definitionService.getConfig(definition, "writing")));
        sections.put("speaking", toPlainObject(definitionService.getConfig(definition, "speaking")));
        sections.put("toeic", toPlainObject(withoutAnswerKey(definitionService.getConfig(definition, "toeic"))));
        response.put("sections", sections);
        attemptRepository.findTopByStudentAndTestCodeOrderBySubmittedAtDesc(student, TEST_CODE)
                .ifPresent(attempt -> response.put("latestAttempt", toResponse(attempt)));
        return response;
    }

    @Transactional
    public PlacementTestAttemptResponse submit(PlacementTestSubmissionRequest request, String studentEmail) {
        User student = requireStudent(studentEmail);
        var definition = definitionService.getDefinition();
        if (!definition.isActive()) {
            throw new IllegalStateException("Bài đánh giá đầu vào hiện đang tạm dừng.");
        }
        String examType = normalizeExamType(request.getExamType() == null ? definition.getExamType() : request.getExamType());
        if ("TOEIC".equals(examType)) {
            validateToeicSubmission(request);
            return submitToeicPlacement(request, student, definition);
        }
        if ("SKILL".equals(examType)) {
            validateSkillAssessmentSubmission(request);
            return submitSkillAssessment(request, student, definition);
        }

        validateSubmission(request);

        JsonNode listeningConfig = definitionService.getConfig(definition, "listening");
        JsonNode readingConfig = definitionService.getConfig(definition, "reading");
        JsonNode listeningAnswers = objectMapper.valueToTree(request.getListeningAnswers());
        JsonNode readingAnswers = objectMapper.valueToTree(request.getReadingAnswers());
        JsonNode writingAnswers = objectMapper.valueToTree(request.getWritingAnswers());
        JsonNode deviceCheck = objectMapper.valueToTree(request.getDeviceCheck());
        ObjectiveScore listening = scoreObjective(listeningAnswers, listeningConfig.path("answerKey"));
        ObjectiveScore reading = scoreObjective(readingAnswers, readingConfig.path("answerKey"));

        BigDecimal listeningBand = listeningBand(listening.correct());
        BigDecimal readingBand = readingBand(reading.correct());
        JsonNode writingConfig = definitionService.getConfig(definition, "writing");
        JsonNode speakingConfig = definitionService.getConfig(definition, "speaking");
        AiEvaluationResult aiResult = evaluateProductiveSkills(request, writingConfig, speakingConfig);
        BigDecimal productiveBand = normalizeBand(aiResult == null ? null : aiResult.getEstimatedScore());
        BigDecimal writingBand = extractBand(aiResult, "writingBand", productiveBand);
        BigDecimal speakingBand = extractBand(aiResult, "speakingBand", productiveBand);
        String status = aiResult == null || (writingBand == null && speakingBand == null) ? "OBJECTIVE_EVALUATED" : "COMPLETED";

        BigDecimal overall = averageAvailable(listeningBand, readingBand, writingBand, speakingBand);
        ObjectNode answers = objectMapper.createObjectNode();
        answers.set("listening", listeningAnswers);
        answers.set("reading", readingAnswers);
        answers.set("writing", writingAnswers);
        answers.put("speakingTranscript", safe(request.getSpeakingTranscript()));
        answers.put("speakingAudioUrl", safe(request.getSpeakingAudioUrl()));

        PlacementTestAttempt attempt = PlacementTestAttempt.builder()
                .student(student)
                .testCode(TEST_CODE)
                .contentBankItem(placementBankItem(definition))
                .answersJson(writeJson(answers))
                .deviceCheckJson(writeJson(deviceCheck))
                .listeningScore(listeningBand)
                .readingScore(readingBand)
                .writingScore(writingBand)
                .speakingScore(speakingBand)
                .overallScore(overall)
                .correctListening(listening.correct())
                .correctReading(reading.correct())
                .aiFeedbackJson(aiResult == null ? fallbackFeedback() : aiResult.getFeedbackJson())
                .status(status)
                .evaluationStatus(PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED)
                .recommendedLevel(null)
                .submittedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(180))
                .build();
        PlacementTestAttempt savedAttempt = attemptRepository.save(attempt);
        student.setCurrentBand(overall == null ? null : overall.doubleValue());
        userRepository.save(student);
        return toResponse(savedAttempt);
    }

    private PlacementTestAttemptResponse submitSkillAssessment(
            PlacementTestSubmissionRequest request,
            User student,
            PlacementTestDefinition definition
    ) {
        Set<AssessmentSkill> selectedSkills = EnumSet.copyOf(request.getSelectedSkills());
        JsonNode listeningAnswers = objectMapper.valueToTree(request.getListeningAnswers());
        JsonNode readingAnswers = objectMapper.valueToTree(request.getReadingAnswers());
        JsonNode writingAnswers = objectMapper.valueToTree(request.getWritingAnswers());
        JsonNode deviceCheck = objectMapper.valueToTree(request.getDeviceCheck());

        ObjectiveScore listening = selectedSkills.contains(AssessmentSkill.LISTENING)
                ? scoreObjective(listeningAnswers, definitionService.getConfig(definition, "listening").path("answerKey"))
                : null;
        ObjectiveScore reading = selectedSkills.contains(AssessmentSkill.READING)
                ? scoreObjective(readingAnswers, definitionService.getConfig(definition, "reading").path("answerKey"))
                : null;
        BigDecimal listeningBand = listening == null ? null : listeningBand(listening.correct());
        BigDecimal readingBand = reading == null ? null : readingBand(reading.correct());

        boolean evaluatesProductiveSkill = selectedSkills.contains(AssessmentSkill.WRITING)
                || selectedSkills.contains(AssessmentSkill.SPEAKING);
        AiEvaluationResult aiResult = evaluatesProductiveSkill
                ? evaluateProductiveSkills(
                        request,
                        definitionService.getConfig(definition, "writing"),
                        definitionService.getConfig(definition, "speaking"),
                        selectedSkills
                )
                : null;
        BigDecimal productiveBand = normalizeBand(aiResult == null ? null : aiResult.getEstimatedScore());
        BigDecimal writingBand = selectedSkills.contains(AssessmentSkill.WRITING)
                ? extractBand(aiResult, "writingBand", productiveBand)
                : null;
        BigDecimal speakingBand = selectedSkills.contains(AssessmentSkill.SPEAKING)
                ? extractBand(aiResult, "speakingBand", productiveBand)
                : null;
        boolean productiveEvaluationComplete = !evaluatesProductiveSkill
                || (selectedSkills.contains(AssessmentSkill.WRITING) ? writingBand != null : true)
                && (selectedSkills.contains(AssessmentSkill.SPEAKING) ? speakingBand != null : true);
        BigDecimal overall = averageAvailable(listeningBand, readingBand, writingBand, speakingBand);

        ObjectNode answers = objectMapper.createObjectNode();
        answers.put("examType", "SKILL");
        var selectedSkillsNode = answers.putArray("selectedSkills");
        selectedSkills.forEach(skill -> selectedSkillsNode.add(skill.name()));
        if (selectedSkills.contains(AssessmentSkill.LISTENING)) answers.set("listening", listeningAnswers);
        if (selectedSkills.contains(AssessmentSkill.READING)) answers.set("reading", readingAnswers);
        if (selectedSkills.contains(AssessmentSkill.WRITING)) answers.set("writing", writingAnswers);
        if (selectedSkills.contains(AssessmentSkill.SPEAKING)) {
            answers.put("speakingTranscript", safe(request.getSpeakingTranscript()));
            answers.put("speakingAudioUrl", safe(request.getSpeakingAudioUrl()));
        }

        PlacementTestAttempt attempt = PlacementTestAttempt.builder()
                .student(student)
                .testCode(TEST_CODE)
                .contentBankItem(placementBankItem(definition))
                .answersJson(writeJson(answers))
                .deviceCheckJson(writeJson(deviceCheck))
                .listeningScore(listeningBand)
                .readingScore(readingBand)
                .writingScore(writingBand)
                .speakingScore(speakingBand)
                .overallScore(overall)
                .correctListening(listening == null ? null : listening.correct())
                .correctReading(reading == null ? null : reading.correct())
                .aiFeedbackJson(skillAssessmentFeedback(aiResult, selectedSkills))
                .status(productiveEvaluationComplete ? "COMPLETED" : "OBJECTIVE_EVALUATED")
                .evaluationStatus(PlacementEvaluationStatus.SUBMITTED)
                .recommendedLevel(null)
                .submittedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(180))
                .build();
        return toResponse(attemptRepository.save(attempt), "SKILL");
    }

    private PlacementTestAttemptResponse submitToeicPlacement(
            PlacementTestSubmissionRequest request,
            User student,
            PlacementTestDefinition definition
    ) {
        JsonNode toeicConfig = definitionService.getConfig(definition, "toeic");
        JsonNode answerKey = toeicConfig.path("answerKey");
        JsonNode listeningAnswers = objectMapper.valueToTree(request.getListeningAnswers());
        JsonNode readingAnswers = objectMapper.valueToTree(request.getReadingAnswers());
        JsonNode deviceCheck = objectMapper.valueToTree(request.getDeviceCheck());

        ObjectiveScore listening = scoreToeicSection(listeningAnswers, answerKey, toeicConfig.path("listening"), 1, 100);
        ObjectiveScore reading = scoreToeicSection(readingAnswers, answerKey, toeicConfig.path("reading"), 101, 200);
        BigDecimal listeningScore = toeicScaledScore(listening);
        BigDecimal readingScore = toeicScaledScore(reading);
        BigDecimal overall = listeningScore.add(readingScore);

        ObjectNode answers = objectMapper.createObjectNode();
        answers.set("listening", listeningAnswers);
        answers.set("reading", readingAnswers);

        PlacementTestAttempt attempt = PlacementTestAttempt.builder()
                .student(student)
                .testCode(TEST_CODE)
                .contentBankItem(placementBankItem(definition))
                .answersJson(writeJson(answers))
                .deviceCheckJson(writeJson(deviceCheck))
                .listeningScore(listeningScore)
                .readingScore(readingScore)
                .writingScore(null)
                .speakingScore(null)
                .overallScore(overall)
                .correctListening(listening.correct())
                .correctReading(reading.correct())
                .aiFeedbackJson("{\"examType\":\"TOEIC\",\"message\":\"Đã chấm khách quan TOEIC Listening & Reading theo answer key.\"}")
                .status("COMPLETED")
                .evaluationStatus(PlacementEvaluationStatus.ELIGIBLE)
                .recommendedLevel(toeicLevel(overall))
                .submittedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(180))
                .build();
        PlacementTestAttempt savedAttempt = attemptRepository.save(attempt);
        student.setCurrentBand(null);
        userRepository.save(student);
        return toResponse(savedAttempt, "TOEIC");
    }

    private ContentBankItem placementBankItem(PlacementTestDefinition definition) {
        if (definition == null || definition.getId() == null) {
            return null;
        }
        return contentBankItemRepository.findById(definition.getId()).orElse(null);
    }

    private ObjectiveScore scoreToeicSection(JsonNode submitted, JsonNode answerKey, JsonNode sectionConfig, int fallbackFrom, int fallbackTo) {
        List<Integer> questionNumbers = toeicQuestionNumbers(sectionConfig);
        if (questionNumbers.isEmpty()) {
            for (int number = fallbackFrom; number <= fallbackTo; number++) {
                questionNumbers.add(number);
            }
        }
        int total = 0;
        int correct = 0;
        for (int number : questionNumbers) {
            JsonNode expected = answerKey.path(String.valueOf(number));
            if (expected.isMissingNode() || expected.isNull()) {
                continue;
            }
            total++;
            if (matches(submitted == null ? null : submitted.get(String.valueOf(number)), expected)) {
                correct++;
            }
        }
        return new ObjectiveScore(correct, total);
    }

    private List<Integer> toeicQuestionNumbers(JsonNode sectionConfig) {
        List<Integer> numbers = new ArrayList<>();
        if (sectionConfig == null || sectionConfig.isMissingNode()) {
            return numbers;
        }
        for (JsonNode part : sectionConfig.withArray("parts")) {
            collectQuestionNumbers(part.withArray("questions"), numbers);
            for (JsonNode group : part.withArray("questionGroups")) {
                if (group.has("questionNumbers")) {
                    for (JsonNode number : group.withArray("questionNumbers")) {
                        if (number.canConvertToInt()) {
                            numbers.add(number.asInt());
                        }
                    }
                }
                collectQuestionNumbers(group.withArray("questions"), numbers);
            }
        }
        return numbers.stream().distinct().sorted().toList();
    }

    private void collectQuestionNumbers(JsonNode questions, List<Integer> numbers) {
        for (JsonNode question : questions) {
            JsonNode number = question.path("number");
            if (number.canConvertToInt()) {
                numbers.add(number.asInt());
            }
        }
    }

    private BigDecimal toeicScaledScore(ObjectiveScore score) {
        if (score.total() <= 0) {
            return BigDecimal.ZERO;
        }
        int scaled = (int) Math.round((score.correct() * 495.0) / score.total());
        scaled = Math.max(5, Math.min(495, Math.round(scaled / 5.0f) * 5));
        return BigDecimal.valueOf(scaled);
    }

    private AiEvaluationResult evaluateProductiveSkills(PlacementTestSubmissionRequest request, JsonNode writingConfig, JsonNode speakingConfig) {
        return evaluateProductiveSkills(
                request,
                writingConfig,
                speakingConfig,
                EnumSet.of(AssessmentSkill.WRITING, AssessmentSkill.SPEAKING)
        );
    }

    private AiEvaluationResult evaluateProductiveSkills(
            PlacementTestSubmissionRequest request,
            JsonNode writingConfig,
            JsonNode speakingConfig,
            Set<AssessmentSkill> selectedSkills
    ) {
        boolean evaluateWriting = selectedSkills.contains(AssessmentSkill.WRITING);
        boolean evaluateSpeaking = selectedSkills.contains(AssessmentSkill.SPEAKING);
        String writing = request.getWritingAnswers() == null ? "" : writeJson(objectMapper.valueToTree(request.getWritingAnswers()));
        String speaking = safe(request.getSpeakingTranscript());

        Optional<AssessmentAudioStorageService.StoredAssessmentAudio> storedAudio =
                evaluateSpeaking && request.getSpeakingAudioUrl() != null && !request.getSpeakingAudioUrl().isBlank()
                        ? audioStorageService.loadStoredAudioFromUrl(request.getSpeakingAudioUrl())
                        : Optional.empty();
        boolean audioAttached = storedAudio.isPresent();

        String speakingAudioPolicy = audioAttached
                ? "The learner's actual speaking audio is attached in this request. Listen to it and judge fluency/coherence, lexical resource, grammar, and pronunciation from the sound. Because real recorded speech is attached, you MUST return a numeric speakingBand on the IELTS 0-9 half-band scale. Use a low band such as 2.0-3.0 when the speech is short, hesitant, or hard to understand. Only set speakingBand to null if the audio is essentially silent or contains no spoken answer at all."
                : "No speaking audio is attached. Judge Speaking only from the transcript text. If the transcript has no real spoken answer, set speakingBand to null.";

        String prompt = """
                You are evaluating an IELTS placement test for course placement, not issuing an official IELTS result.
                Score the learner on the IELTS 0-9 band scale using half-band increments.
                Evaluate only these selected productive skills: %s.
                For Writing, evaluate task response, coherence, lexical resource, grammar.
                For Speaking, evaluate fluency/coherence, lexical resource, grammar, pronunciation evidence when audio is available.
                Return null for a productive skill that is not selected.
                If a response is clearly off-topic, nonsensical, irrelevant to the prompt, or made of filler unrelated to the actual task, assign 0.0 for that skill.
                If there is not enough real evidence to judge Writing reliably, set writingBand to 0.0 when the writing is present but clearly irrelevant, or null only when no meaningful writing content exists.
                Speaking scoring policy: %s
                Return concise JSON feedback with keys: estimatedScore, writingBand, speakingBand, strengths, weaknesses, recommendations.

                WRITING TASKS:
                %s

                WRITING RESPONSES:
                %s

                SPEAKING TASK:
                %s

//                SPEAKING TRANSCRIPT:
                %s
                """.formatted(
                        selectedSkills.stream().map(Enum::name).sorted().toList(),
                        speakingAudioPolicy,
                        evaluateWriting ? writeJson(writingConfig) : "Not selected",
                        evaluateWriting ? writing : "Not selected",
                        evaluateSpeaking ? writeJson(speakingConfig) : "Not selected",
                        evaluateSpeaking ? speaking : "Not selected"
                );
        try {
            AiEvaluationResult aiResult = audioAttached
                    ? aiEvaluationClient.evaluateWithAudio(prompt, storedAudio.get().bytes(), storedAudio.get().contentType())
                    : aiEvaluationClient.evaluate(prompt);
            return applyProductiveGuards(aiResult, request, selectedSkills);
        } catch (RuntimeException exception) {
            log.warn("Placement test AI evaluation for writing/speaking failed; falling back to objective-only result. Reason: {}",
                    exception.getMessage(), exception);
            return null;
        }
    }

    private AiEvaluationResult applyProductiveGuards(
            AiEvaluationResult aiResult,
            PlacementTestSubmissionRequest request,
            Set<AssessmentSkill> selectedSkills
    ) {
        if (aiResult == null) {
            return null;
        }

        try {
            ObjectNode root = aiResult.getFeedbackJson() == null || aiResult.getFeedbackJson().isBlank()
                    ? objectMapper.createObjectNode()
                    : (ObjectNode) objectMapper.readTree(aiResult.getFeedbackJson());

            BigDecimal writingBand = selectedSkills.contains(AssessmentSkill.WRITING)
                    ? readBand(root.path("writingBand"), normalizeBand(aiResult.getEstimatedScore()))
                    : null;
            BigDecimal speakingBand = selectedSkills.contains(AssessmentSkill.SPEAKING)
                    ? readBand(root.path("speakingBand"), normalizeBand(aiResult.getEstimatedScore()))
                    : null;

            WritingEvidence writingEvidence = selectedSkills.contains(AssessmentSkill.WRITING)
                    ? evaluateWritingEvidence(request.getWritingAnswers())
                    : null;
            SpeakingEvidence speakingEvidence = selectedSkills.contains(AssessmentSkill.SPEAKING)
                    ? evaluateSpeakingEvidence(request, aiResult.isAudioInputAnalyzed())
                    : null;

            if (writingEvidence != null && writingEvidence.offTopicAllTasks()) {
                writingBand = band(0);
                appendGuardFeedback(root,
                        "Phần Writing đang lệch đề nặng hoặc nội dung không liên quan tới cả hai task, nên bị chấm 0.",
                        "Viết lại đúng trọng tâm: Task 1 phải mô tả quy trình sản xuất ethanol từ ngô; Task 2 phải bàn về physical strength và mental strength trong thể thao.");
            } else if (writingEvidence != null && writingEvidence.hasSevereProblem()) {
                writingBand = minBand(writingBand, band(2.5));
                appendGuardFeedback(root,
                        "Phần Writing có ít nhất một task quá ngắn hoặc lệch đề rõ rệt, nên điểm bị hạ mạnh.",
                        "Hoàn thành đầy đủ cả hai task, bám đúng đề và phát triển ý rõ ràng trước khi nộp lại.");
            }

            if (speakingEvidence != null && speakingEvidence.insufficientEvidence()) {
                speakingBand = null;
                appendGuardFeedback(root,
                        speakingEvidence.message(),
                        "Hãy nộp lại bài nói với bản ghi thật rõ hoặc transcript thực sự phản ánh câu trả lời của bạn.");
            } else if (speakingEvidence != null && speakingEvidence.offTopic()) {
                speakingBand = band(0);
                appendGuardFeedback(root,
                        "Phần Speaking lệch đề nặng hoặc nội dung nói không liên quan tới các câu hỏi đã cho, nên bị chấm 0.",
                        "Trả lời trực tiếp câu hỏi Part 1, mô tả đúng cue card ở Part 2, và bám chủ đề leisure / work / activities ở Part 3.");
            }

            BigDecimal productiveAverage = averageAvailable(writingBand, speakingBand);
            aiResult.setEstimatedScore(productiveAverage);
            if (productiveAverage == null) {
                root.putNull("estimatedScore");
            } else {
                root.put("estimatedScore", productiveAverage);
            }

            if (writingBand == null) {
                root.putNull("writingBand");
            } else {
                root.put("writingBand", writingBand);
            }

            if (speakingBand == null) {
                root.putNull("speakingBand");
            } else {
                root.put("speakingBand", speakingBand);
            }

            aiResult.setFeedbackJson(objectMapper.writeValueAsString(root));
            return aiResult;
        } catch (Exception exception) {
            return aiResult;
        }
    }

    private ObjectiveScore scoreObjective(JsonNode submitted, JsonNode answerKey) {
        int total = 0;
        int correct = 0;
        Iterator<Map.Entry<String, JsonNode>> fields = answerKey.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            total++;
            if (matches(submitted == null ? null : submitted.get(entry.getKey()), entry.getValue())) {
                correct++;
            }
        }
        return new ObjectiveScore(correct, total);
    }

    private boolean matches(JsonNode actual, JsonNode expected) {
        if (actual == null || actual.isNull() || actual.asText().isBlank()) return false;
        if (expected.isArray()) {
            if (actual.isArray()) {
                Set<String> actualValues = new HashSet<>();
                actual.forEach(value -> actualValues.add(normalize(value.asText())));
                Set<String> expectedValues = new HashSet<>();
                expected.forEach(value -> expectedValues.add(normalize(value.asText())));
                return actualValues.equals(expectedValues);
            }
            for (JsonNode candidate : expected) {
                if (normalize(actual.asText()).equals(normalize(candidate.asText()))) return true;
            }
            return false;
        }
        return normalize(actual.asText()).equals(normalize(expected.asText()));
    }

    private String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT)
                .replaceAll("[£$,.]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private BigDecimal listeningBand(int correct) {
        if (correct >= 39) return band(9);
        if (correct >= 37) return band(8.5);
        if (correct >= 35) return band(8);
        if (correct >= 33) return band(7.5);
        if (correct >= 30) return band(7);
        if (correct >= 27) return band(6.5);
        if (correct >= 23) return band(6);
        if (correct >= 20) return band(5.5);
        if (correct >= 16) return band(5);
        if (correct >= 13) return band(4.5);
        if (correct >= 10) return band(4);
        if (correct >= 7) return band(3.5);
        if (correct >= 5) return band(3);
        if (correct >= 3) return band(2.5);
        return band(0);
    }

    private BigDecimal readingBand(int correct) {
        if (correct >= 40) return band(9);
        if (correct >= 39) return band(8.5);
        if (correct >= 38) return band(8);
        if (correct >= 36) return band(7.5);
        if (correct >= 34) return band(7);
        if (correct >= 32) return band(6.5);
        if (correct >= 30) return band(6);
        if (correct >= 27) return band(5.5);
        if (correct >= 23) return band(5);
        if (correct >= 19) return band(4.5);
        if (correct >= 15) return band(4);
        if (correct >= 12) return band(3.5);
        if (correct >= 8) return band(3);
        if (correct >= 5) return band(2.5);
        return band(0);
    }

    private BigDecimal normalizeBand(BigDecimal value) {
        return IeltsBandScale.normalizeBand(value);
    }

    private BigDecimal extractBand(AiEvaluationResult result, String field, BigDecimal fallback) {
        if (result == null || result.getFeedbackJson() == null || result.getFeedbackJson().isBlank()) return fallback;
        try {
            JsonNode value = objectMapper.readTree(result.getFeedbackJson()).path(field);
            return readBand(value, fallback);
        } catch (IOException exception) {
            return fallback;
        }
    }

    private BigDecimal readBand(JsonNode value, BigDecimal fallback) {
        if (value == null || value.isMissingNode()) {
            return fallback;
        }
        if (value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return normalizeBand(value.decimalValue());
        }
        String raw = safe(value.asText());
        if (raw.isBlank()) {
            return null;
        }
        try {
            return normalizeBand(new BigDecimal(raw));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private BigDecimal averageAvailable(BigDecimal... values) {
        List<BigDecimal> available = Arrays.stream(values).filter(Objects::nonNull).toList();
        if (available.isEmpty()) return null;
        BigDecimal total = available.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return normalizeBand(total.divide(BigDecimal.valueOf(available.size()), 2, RoundingMode.HALF_UP));
    }

    private BigDecimal band(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP);
    }

    private BigDecimal minBand(BigDecimal current, BigDecimal cap) {
        if (cap == null) return current;
        if (current == null) return cap;
        return current.compareTo(cap) <= 0 ? current : cap;
    }

    private WritingEvidence evaluateWritingEvidence(Map<String, Object> writingAnswers) {
        String task1 = safe(asText(writingAnswers == null ? null : writingAnswers.get("task_1")));
        String task2 = safe(asText(writingAnswers == null ? null : writingAnswers.get("task_2")));

        int task1Words = countWords(task1);
        int task2Words = countWords(task2);
        int task1Hits = countKeywordHits(task1, WRITING_TASK_1_KEYWORDS);
        int task2Hits = countKeywordHits(task2, WRITING_TASK_2_KEYWORDS);

        boolean task1OffTopic = task1Words >= 25 && task1Hits == 0;
        boolean task2OffTopic = task2Words >= 40 && task2Hits == 0;
        boolean task1TooShort = task1Words < 40;
        boolean task2TooShort = task2Words < 60;

        return new WritingEvidence(task1OffTopic, task2OffTopic, task1TooShort, task2TooShort);
    }

    private SpeakingEvidence evaluateSpeakingEvidence(PlacementTestSubmissionRequest request, boolean audioAnalyzed) {
        String transcript = safe(request.getSpeakingTranscript());
        boolean hasAudioUrl = request.getSpeakingAudioUrl() != null && !request.getSpeakingAudioUrl().isBlank();
        boolean metadataOnlyTranscript = SPEAKING_METADATA_PATTERN.matcher(transcript).find();
        int transcriptWords = countWords(transcript);

        if (!audioAnalyzed) {
            if ((!hasAudioUrl && transcriptWords < 20) || metadataOnlyTranscript) {
                return new SpeakingEvidence(false, true,
                        "Phần Speaking chưa có đủ bằng chứng nói thật để chấm: transcript quá ít hoặc chỉ là metadata của bài thi.");
            }
            if (transcriptWords < 20) {
                return new SpeakingEvidence(false, true,
                        "Phần Speaking quá ngắn nên chưa đủ bằng chứng để chấm đáng tin cậy.");
            }
            if (countKeywordHits(transcript, SPEAKING_TOPIC_KEYWORDS) == 0) {
                return new SpeakingEvidence(true, false,
                        "Phần Speaking lệch khỏi chủ đề của đề bài.");
            }
        }

        return new SpeakingEvidence(false, false, "");
    }

    private int countWords(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) Arrays.stream(text.trim().split("\\s+")).filter(token -> !token.isBlank()).count();
    }

    private int countKeywordHits(String text, Set<String> keywords) {
        String normalizedText = " " + normalizeForRelevance(text) + " ";
        int hits = 0;
        for (String keyword : keywords) {
            String needle = " " + normalizeForRelevance(keyword) + " ";
            if (normalizedText.contains(needle)) {
                hits++;
            }
        }
        return hits;
    }

    private String normalizeForRelevance(String value) {
        return safe(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private void appendGuardFeedback(ObjectNode root, String weakness, String suggestion) {
        appendArrayText(root, "weaknesses", weakness);
        appendArrayText(root, "recommendations", suggestion);
        appendArrayText(root, "suggestions", suggestion);
    }

    private void appendArrayText(ObjectNode root, String field, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        var node = root.withArray(field);
        for (JsonNode item : node) {
            if (value.equalsIgnoreCase(item.asText())) {
                return;
            }
        }
        node.add(value);
    }

    private JsonNode withoutAnswerKey(JsonNode source) {
        ObjectNode copy = source.deepCopy();
        copy.remove("answerKey");
        return copy;
    }

    private String writeJson(JsonNode node) {
        if (node == null) return null;
        try {
            return objectMapper.writeValueAsString(node);
        } catch (IOException exception) {
            throw new IllegalStateException("Không thể lưu dữ liệu bài đánh giá đầu vào.", exception);
        }
    }

    private void validateSubmission(PlacementTestSubmissionRequest request) {
        if (request == null || request.getListeningAnswers() == null || request.getReadingAnswers() == null
                || request.getWritingAnswers() == null) {
            throw new RuntimeException("Bài thi chưa có đủ dữ liệu của bốn kỹ năng.");
        }
        if (request.getDeviceCheck() == null || !Boolean.TRUE.equals(request.getDeviceCheck().get("completed"))) {
            throw new RuntimeException("Bạn cần hoàn thành kiểm tra thiết bị trước khi nộp bài.");
        }
        if ((request.getSpeakingTranscript() == null || request.getSpeakingTranscript().isBlank())
                && (request.getSpeakingAudioUrl() == null || request.getSpeakingAudioUrl().isBlank())) {
            throw new RuntimeException("Phần Speaking cần có bản ghi âm hoặc nội dung trả lời.");
        }
    }

    private void validateToeicSubmission(PlacementTestSubmissionRequest request) {
        if (request == null || request.getListeningAnswers() == null || request.getReadingAnswers() == null) {
            throw new RuntimeException("Bài TOEIC chưa có đủ dữ liệu Listening và Reading.");
        }
        if (request.getDeviceCheck() == null || !Boolean.TRUE.equals(request.getDeviceCheck().get("completed"))) {
            throw new RuntimeException("Bạn cần hoàn thành kiểm tra thiết bị trước khi nộp bài.");
        }
    }

    private void validateSkillAssessmentSubmission(PlacementTestSubmissionRequest request) {
        if (request == null || request.getSelectedSkills() == null || request.getSelectedSkills().isEmpty()) {
            throw new RuntimeException("Hãy chọn ít nhất một kỹ năng cần đánh giá.");
        }
        if (request.getSelectedSkills().stream().anyMatch(Objects::isNull)) {
            throw new RuntimeException("Danh sách kỹ năng được chọn không hợp lệ.");
        }
        Set<AssessmentSkill> selectedSkills = EnumSet.copyOf(request.getSelectedSkills());
        if (selectedSkills.size() != request.getSelectedSkills().size()
                || selectedSkills.contains(AssessmentSkill.MIXED)) {
            throw new RuntimeException("Danh sách kỹ năng được chọn không hợp lệ.");
        }
        if (request.getDeviceCheck() == null || !Boolean.TRUE.equals(request.getDeviceCheck().get("completed"))) {
            throw new RuntimeException("Bạn cần hoàn thành kiểm tra thiết bị trước khi nộp bài.");
        }
        if (selectedSkills.contains(AssessmentSkill.LISTENING) && request.getListeningAnswers() == null) {
            throw new RuntimeException("Phần Listening chưa có dữ liệu bài làm.");
        }
        if (selectedSkills.contains(AssessmentSkill.READING) && request.getReadingAnswers() == null) {
            throw new RuntimeException("Phần Reading chưa có dữ liệu bài làm.");
        }
        if (selectedSkills.contains(AssessmentSkill.WRITING) && request.getWritingAnswers() == null) {
            throw new RuntimeException("Phần Writing chưa có dữ liệu bài làm.");
        }
        if (selectedSkills.contains(AssessmentSkill.SPEAKING)
                && (request.getSpeakingTranscript() == null || request.getSpeakingTranscript().isBlank())
                && (request.getSpeakingAudioUrl() == null || request.getSpeakingAudioUrl().isBlank())) {
            throw new RuntimeException("Phần Speaking cần có bản ghi âm hoặc nội dung trả lời.");
        }
    }

    private String normalizeExamType(String value) {
        String normalized = safe(value).toUpperCase(Locale.ROOT);
        if ("TOEIC".equals(normalized)) return "TOEIC";
        if ("SKILL".equals(normalized)) return "SKILL";
        return "IELTS";
    }

    private User requireStudent(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
    }

    private PlacementTestAttemptResponse toResponse(PlacementTestAttempt attempt) {
        return toResponse(attempt, null);
    }

    private PlacementTestAttemptResponse toResponse(PlacementTestAttempt attempt, String examType) {
        String resolvedExamType = examType != null
                ? examType
                : resolveStoredExamType(attempt);
        return PlacementTestAttemptResponse.builder()
                .id(attempt.getId())
                .learnerId(attempt.getStudent().getId())
                .learnerName(attempt.getStudent().getFullName())
                .learnerEmail(attempt.getStudent().getEmail())
                .testCode(attempt.getTestCode())
                .examType(resolvedExamType)
                .selectedSkills(resolveSelectedSkills(attempt, resolvedExamType))
                .listeningScore(attempt.getListeningScore())
                .readingScore(attempt.getReadingScore())
                .writingScore(attempt.getWritingScore())
                .speakingScore(attempt.getSpeakingScore())
                .overallScore(attempt.getOverallScore())
                .correctListening(attempt.getCorrectListening())
                .correctReading(attempt.getCorrectReading())
                .aiFeedbackJson(attempt.getAiFeedbackJson())
                .status(attempt.getStatus())
                .evaluationStatus(attempt.getEvaluationStatus())
                .recommendedLevel(attempt.getRecommendedLevel())
                .expiresAt(attempt.getExpiresAt())
                .reviewerId(attempt.getReviewer() == null ? null : attempt.getReviewer().getId())
                .reviewerName(attempt.getReviewer() == null ? null : attempt.getReviewer().getFullName())
                .reviewedAt(attempt.getReviewedAt())
                .reviewNote(attempt.getReviewNote())
                .submittedAt(attempt.getSubmittedAt())
                .build();
    }

    private PlacementLevel toeicLevel(BigDecimal score) {
        if (score == null || score.compareTo(BigDecimal.valueOf(450)) < 0) {
            return PlacementLevel.BEGINNER;
        }
        if (score.compareTo(BigDecimal.valueOf(700)) < 0) {
            return PlacementLevel.INTERMEDIATE;
        }
        return PlacementLevel.ADVANCED;
    }

    private String resolveStoredExamType(PlacementTestAttempt attempt) {
        String feedback = safe(attempt.getAiFeedbackJson());
        if (feedback.contains("\"examType\":\"SKILL\"")) {
            return "SKILL";
        }
        if (feedback.contains("\"examType\":\"TOEIC\"")) {
            return "TOEIC";
        }
        return attempt.getOverallScore() != null && attempt.getOverallScore().compareTo(BigDecimal.valueOf(9)) > 0
                ? "TOEIC"
                : "IELTS";
    }

    private List<AssessmentSkill> resolveSelectedSkills(PlacementTestAttempt attempt, String examType) {
        if ("TOEIC".equals(examType)) {
            return List.of(AssessmentSkill.LISTENING, AssessmentSkill.READING);
        }
        if (!"SKILL".equals(examType)) {
            return List.of(
                    AssessmentSkill.LISTENING,
                    AssessmentSkill.READING,
                    AssessmentSkill.WRITING,
                    AssessmentSkill.SPEAKING
            );
        }
        try {
            JsonNode root = objectMapper.readTree(attempt.getAnswersJson());
            List<AssessmentSkill> selected = new ArrayList<>();
            for (JsonNode value : root.withArray("selectedSkills")) {
                selected.add(AssessmentSkill.valueOf(value.asText().toUpperCase(Locale.ROOT)));
            }
            return List.copyOf(selected);
        } catch (Exception ignored) {
            List<AssessmentSkill> inferred = new ArrayList<>();
            if (attempt.getListeningScore() != null) inferred.add(AssessmentSkill.LISTENING);
            if (attempt.getReadingScore() != null) inferred.add(AssessmentSkill.READING);
            if (attempt.getWritingScore() != null) inferred.add(AssessmentSkill.WRITING);
            if (attempt.getSpeakingScore() != null) inferred.add(AssessmentSkill.SPEAKING);
            return List.copyOf(inferred);
        }
    }

    private String skillAssessmentFeedback(AiEvaluationResult aiResult, Set<AssessmentSkill> selectedSkills) {
        try {
            ObjectNode root = aiResult == null || aiResult.getFeedbackJson() == null || aiResult.getFeedbackJson().isBlank()
                    ? objectMapper.createObjectNode()
                    : (ObjectNode) objectMapper.readTree(aiResult.getFeedbackJson());
            root.put("examType", "SKILL");
            var selectedSkillsNode = root.putArray("selectedSkills");
            selectedSkills.forEach(skill -> selectedSkillsNode.add(skill.name()));
            if (aiResult == null) {
                root.put("message", "Đã chấm các kỹ năng khách quan. Kỹ năng tự luận sẽ được chấm lại khi dịch vụ AI sẵn sàng.");
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể lưu kết quả đánh giá kỹ năng.", exception);
        }
    }

    private String fallbackFeedback() {
        return "{\"message\":\"Đã lưu bài và chấm hai kỹ năng khách quan. Writing và Speaking sẽ được chấm lại khi dịch vụ AI sẵn sàng.\"}";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toPlainObject(JsonNode node) {
        return objectMapper.convertValue(node, Map.class);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private record ObjectiveScore(int correct, int total) {
    }

    private record WritingEvidence(boolean task1OffTopic, boolean task2OffTopic, boolean task1TooShort, boolean task2TooShort) {
        private boolean offTopicAllTasks() {
            return task1OffTopic && task2OffTopic;
        }

        private boolean hasSevereProblem() {
            return task1OffTopic || task2OffTopic || task1TooShort || task2TooShort;
        }
    }

    private record SpeakingEvidence(boolean offTopic, boolean insufficientEvidence, String message) {
    }
}
