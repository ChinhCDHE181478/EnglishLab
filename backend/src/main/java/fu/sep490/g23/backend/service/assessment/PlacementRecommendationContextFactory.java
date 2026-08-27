package fu.sep490.g23.backend.service.assessment;

import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementLevel;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Snapshot of scores, weak skills, and learner target used by recommendation ranking.
 */
@Component
public class PlacementRecommendationContextFactory {
    private static final Pattern NUMBER = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    /** Pack attempt scores + learner target into one ranking input. Nested: resolveExamType, resolveWeakSkills, parseScore. */
    public PlacementRecommendationContext fromAttempt(
            User learner,
            PlacementTestAttempt attempt,
            PlacementLevel recommendedLevel
    ) {
        String examType = resolveExamType(attempt, learner == null ? null : learner.getTargetExam());
        return PlacementRecommendationContext.builder()
                .learnerId(learner == null ? null : learner.getId())
                .attemptId(attempt.getId())
                .examType(examType)
                .overallScore(attempt.getOverallScore())
                .listeningScore(attempt.getListeningScore())
                .readingScore(attempt.getReadingScore())
                .writingScore(attempt.getWritingScore())
                .speakingScore(attempt.getSpeakingScore())
                .recommendedLevel(recommendedLevel != null ? recommendedLevel : attempt.getRecommendedLevel())
                .weakSkills(resolveWeakSkills(attempt, examType))
                .targetExam(normalizeExam(learner == null ? null : learner.getTargetExam(), examType))
                .targetScore(parseScore(learner == null ? null : learner.getTargetScore()))
                .build();
    }

    /**
     * Find skills that need work.
     * 1) Take the lowest skill score, then include every skill within +0.5 of it.
     * 2) If no numeric scores, scan AI feedback text for skill names (TOEIC = L/R only).
     */
    public Set<AssessmentSkill> resolveWeakSkills(PlacementTestAttempt attempt, String examType) {
        if (attempt == null) return Set.of();
        Map<AssessmentSkill, BigDecimal> scores = new EnumMap<>(AssessmentSkill.class);
        putScore(scores, AssessmentSkill.LISTENING, attempt.getListeningScore());
        putScore(scores, AssessmentSkill.READING, attempt.getReadingScore());
        if (!"TOEIC".equals(examType)) {
            putScore(scores, AssessmentSkill.WRITING, attempt.getWritingScore());
            putScore(scores, AssessmentSkill.SPEAKING, attempt.getSpeakingScore());
        }

        Set<AssessmentSkill> weakSkills = new LinkedHashSet<>();
        // Weak = lowest score, plus any skill within 0.5 of that lowest.
        scores.values().stream().min(BigDecimal::compareTo).ifPresent(minimum -> scores.forEach((skill, score) -> {
            if (score.compareTo(minimum.add(BigDecimal.valueOf(0.5))) <= 0) weakSkills.add(skill);
        }));
        if (!weakSkills.isEmpty()) return Collections.unmodifiableSet(new LinkedHashSet<>(weakSkills));

        String feedback = String.valueOf(attempt.getAiFeedbackJson()).toUpperCase(Locale.ROOT);
        List<AssessmentSkill> candidates = "TOEIC".equals(examType)
                ? List.of(AssessmentSkill.LISTENING, AssessmentSkill.READING)
                : List.of(AssessmentSkill.LISTENING, AssessmentSkill.READING, AssessmentSkill.WRITING, AssessmentSkill.SPEAKING);
        candidates.stream().filter(skill -> feedback.contains(skill.name())).forEach(weakSkills::add);
        return Collections.unmodifiableSet(new LinkedHashSet<>(weakSkills));
    }

    /**
     * Detect exam type from stored AI JSON first, then testCode, then learner target, else IELTS.
     * SKILL is checked before TOEIC because both strings can appear in feedback.
     */
    public String resolveExamType(PlacementTestAttempt attempt, String fallback) {
        String feedback = String.valueOf(attempt == null ? null : attempt.getAiFeedbackJson()).toUpperCase(Locale.ROOT);
        if (feedback.contains("\"EXAMTYPE\":\"SKILL\"")) return "SKILL";
        if (feedback.contains("\"EXAMTYPE\":\"TOEIC\"") || feedback.contains("TOEIC")) return "TOEIC";
        if (feedback.contains("\"EXAMTYPE\":\"IELTS\"") || feedback.contains("IELTS")) return "IELTS";
        String testCode = String.valueOf(attempt == null ? null : attempt.getTestCode()).toUpperCase(Locale.ROOT);
        if (testCode.contains("TOEIC")) return "TOEIC";
        return normalizeExam(fallback, "IELTS");
    }

    /** Keep IELTS / TOEIC / SKILL; anything else falls back (usually the attempt exam type). */
    private String normalizeExam(String value, String fallback) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        return Set.of("IELTS", "TOEIC", "SKILL").contains(normalized) ? normalized : fallback;
    }

    /** Pull the first number from free-text targetScore like "6.5" or "TOEIC 700". */
    private BigDecimal parseScore(String value) {
        Matcher matcher = NUMBER.matcher(value == null ? "" : value);
        if (!matcher.find()) return null;
        try {
            return new BigDecimal(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    /** Ignore null skill scores so they do not count as 0 (which would look like the weakest skill). */
    private void putScore(Map<AssessmentSkill, BigDecimal> scores, AssessmentSkill skill, BigDecimal score) {
        if (score != null) scores.put(skill, score);
    }
}
