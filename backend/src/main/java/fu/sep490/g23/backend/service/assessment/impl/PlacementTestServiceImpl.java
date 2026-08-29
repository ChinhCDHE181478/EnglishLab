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

/**
 * Loads the placement paper and scores a student's submission.
 *
 * Flow: validate -> score Listening/Reading from answer keys
 * -> AI-score Writing/Speaking (plus local off-topic guards) -> save attempt.
 * IELTS waits for staff review; TOEIC is eligible immediately; SKILL is diagnostic only.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlacementTestServiceImpl implements PlacementTestService {
    private static final String TEST_CODE = PlacementTestDefinitionService.TEST_CODE;
    // Transcript that is only device metadata, not a real spoken answer.
    private static final Pattern SPEAKING_METADATA_PATTERN = Pattern.compile("speaking mock test:|part prompts shown to the learner:|recording duration seconds:|voice signal detected:", Pattern.CASE_INSENSITIVE);
    // Topic words used to flag off-topic Writing/Speaking locally (before trusting AI).
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



    /**
     * Ask the AI client to band Writing/Speaking.
     * Attach the stored recording when available;


    /** Cap or zero AI bands when local checks show off-topic / too-short / no real speech. */
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
                writingBand = band(0); // Both tasks ignore the prompt -> writing is 0.
                appendGuardFeedback(root,
                        "Phần Writing đang lệch đề nặng hoặc nội dung không liên quan tới cả hai task, nên bị chấm 0.",
                        "Viết lại đúng trọng tâm: Task 1 phải mô tả quy trình sản xuất ethanol từ ngô; Task 2 phải bàn về physical strength và mental strength trong thể thao.");
            } else if (writingEvidence != null && writingEvidence.hasSevereProblem()) {
                writingBand = minBand(writingBand, band(2.5)); // Cap AI score when a task is too short / off-topic.
                appendGuardFeedback(root,
                        "Phần Writing có ít nhất một task quá ngắn hoặc lệch đề rõ rệt, nên điểm bị hạ mạnh.",
                        "Hoàn thành đầy đủ cả hai task, bám đúng đề và phát triển ý rõ ràng trước khi nộp lại.");
            }

            if (speakingEvidence != null && speakingEvidence.insufficientEvidence()) {
                speakingBand = null; // Do not invent a speaking band without real speech evidence.
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
            aiResult.setEstimatedScore(productiveAverage); // Rewrite AI JSON so stored feedback matches the guarded bands.
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


    /** Accept a single value or a set;


    private String normalize(String value) {
        return safe(value).toLowerCase(Locale.ROOT)
                .replaceAll("[£$,.]", "")
                .replaceAll("\\s+", " ")
                .trim();
    }



    /** Read writingBand / speakingBand from AI JSON;


    /** Average of the skill bands that exist; skip nulls (unselected / unscored skills). */
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


    private String asText(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    /** Append a weakness + suggestion into the AI feedback JSON (no duplicates). */
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

    /** Never send the answer key to the student client. */
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




    private User requireStudent(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
    }

    private PlacementTestAttemptResponse toResponse(PlacementTestAttempt attempt) {
        return toResponse(attempt, null);
    }

    /** Map a saved attempt to the API response, inferring exam type / selected skills if needed. */
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

    /** Local speaking checks used to override an AI band. */
    private record SpeakingEvidence(boolean offTopic, boolean insufficientEvidence, String message) {
    }
}
