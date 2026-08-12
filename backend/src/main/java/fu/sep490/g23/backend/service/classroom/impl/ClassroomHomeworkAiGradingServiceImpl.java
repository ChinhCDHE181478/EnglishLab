package fu.sep490.g23.backend.service.classroom.impl;
import fu.sep490.g23.backend.service.classroom.ClassroomHomeworkAiGradingService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fu.sep490.g23.backend.entity.assessment.AssessmentRubric;
import fu.sep490.g23.backend.entity.assessment.RubricCriterion;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomework;
import fu.sep490.g23.backend.entity.classroom.ClassroomHomeworkSubmission;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkGradingMode;
import fu.sep490.g23.backend.entity.classroom.enums.HomeworkSubmissionStatus;
import fu.sep490.g23.backend.service.ai.AiEvaluationClient;
import fu.sep490.g23.backend.service.ai.AiEvaluationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassroomHomeworkAiGradingServiceImpl implements ClassroomHomeworkAiGradingService {

    private final AiEvaluationClient aiEvaluationClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean tryAutoGrade(ClassroomHomeworkSubmission submission) {
        ClassroomHomework homework = submission.getHomework();
        if (homework.getGradingMode() != HomeworkGradingMode.AI) {
            return false;
        }
        if (homework.getRubric() == null) {
            return false;
        }
        if (submission.getTextAnswer() == null || submission.getTextAnswer().isBlank()) {
            if (submission.getAttachmentUrl() == null || submission.getAttachmentUrl().isBlank()) {
                return false;
            }
        }

        try {
            String prompt = buildPrompt(homework, submission);
            AiEvaluationResult result = aiEvaluationClient.evaluate(prompt);
            applyAiResult(submission, homework, result);
            return true;
        } catch (Exception ex) {
            log.warn("Không thể chấm AI bài tập homeworkId={}: {}", homework.getId(), ex.getMessage());
            return false;
        }
    }

    private void applyAiResult(
            ClassroomHomeworkSubmission submission,
            ClassroomHomework homework,
            AiEvaluationResult result
    ) {
        BigDecimal score = normalizeScore(result.getEstimatedScore(), homework.getMaxScore(), homework.getSkill());
        String feedback = extractFeedbackSummary(result.getFeedbackJson());

        submission.setScore(score);
        submission.setTeacherFeedback(feedback);
        submission.setGradedAt(LocalDateTime.now());
        submission.setGradedBy(null);
        submission.setStatus(HomeworkSubmissionStatus.GRADED);
    }

    private BigDecimal normalizeScore(BigDecimal rawScore, BigDecimal maxScore, AssessmentSkill skill) {
        BigDecimal cap = maxScore == null ? BigDecimal.TEN : maxScore;
        if (rawScore == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (skill == AssessmentSkill.SPEAKING || skill == AssessmentSkill.WRITING) {
            // AI band 0-9 -> scale to homework max
            BigDecimal scaled = rawScore.multiply(cap).divide(BigDecimal.valueOf(9), 2, RoundingMode.HALF_UP);
            return clamp(scaled, cap);
        }
        if (skill == AssessmentSkill.VOCABULARY) {
            // rubric 0-10
            BigDecimal scaled = rawScore.multiply(cap).divide(BigDecimal.TEN, 2, RoundingMode.HALF_UP);
            return clamp(scaled, cap);
        }
        return clamp(rawScore, cap);
    }

    private BigDecimal clamp(BigDecimal value, BigDecimal max) {
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        if (value.compareTo(max) > 0) {
            return max.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private String extractFeedbackSummary(String feedbackJson) {
        if (feedbackJson == null || feedbackJson.isBlank()) {
            return "AI đã chấm bài. Vui lòng xem chi tiết trong hệ thống.";
        }
        try {
            JsonNode root = objectMapper.readTree(feedbackJson);
            String summary = root.path("summary").asText("");
            if (!summary.isBlank()) {
                return summary;
            }
            JsonNode strengths = root.path("strengths");
            if (strengths.isArray() && !strengths.isEmpty()) {
                return strengths.get(0).asText("AI đã chấm bài.");
            }
        } catch (Exception ignored) {
            // fall through
        }
        return feedbackJson.length() > 1200 ? feedbackJson.substring(0, 1200) + "..." : feedbackJson;
    }

    private String buildPrompt(ClassroomHomework homework, ClassroomHomeworkSubmission submission) {
        AssessmentRubric rubric = homework.getRubric();
        String criteriaBlock = rubric.getCriteria().stream()
                .sorted(Comparator.comparing(RubricCriterion::getDisplayOrder).thenComparing(RubricCriterion::getId))
                .map(criterion -> "- " + criterion.getName() + " (weight " + criterion.getWeight() + "%): "
                        + criterion.getDescription() + " | " + criterion.getBandDescriptors())
                .collect(Collectors.joining("\n"));

        return """
                You are grading a classroom homework submission for EnglishLab.
                Respond ONLY with valid JSON matching this schema:
                {
                  "estimatedScore": number,
                  "summary": "Vietnamese feedback summary",
                  "criterionFeedback": [{"name":"...", "feedback":"Vietnamese"}],
                  "strengths": ["..."],
                  "improvements": ["..."]
                }

                Homework title: %s
                Skill: %s
                Rubric: %s
                Scoring scale: %s
                Max homework score: %s
                Instructions:
                %s

                Rubric criteria:
                %s

                Student submission:
                %s

                Rules:
                - Write summary and feedback in Vietnamese.
                - estimatedScore must fit the rubric scoring scale before mapping to homework max score.
                - For Speaking/Writing use IELTS-like band 0-9 in estimatedScore.
                - For Vocabulary use 0-10 in estimatedScore.
                - For Listening/Reading estimate accuracy percentage 0-100 if no official answer key is provided in instructions.
                """.formatted(
                safe(homework.getTitle()),
                homework.getSkill() == null ? "GENERAL" : homework.getSkill().name(),
                safe(rubric.getName()),
                safe(rubric.getScoringScale()),
                homework.getMaxScore() == null ? "10" : homework.getMaxScore().toPlainString(),
                safe(homework.getInstruction()),
                criteriaBlock,
                safe(firstNonBlank(submission.getTextAnswer(), submission.getAttachmentUrl()))
        );
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
