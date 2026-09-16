package fu.sep490.g23.backend.service.assessment.impl;

import fu.sep490.g23.backend.dto.response.assessment.PlacementEligibilityResult;
import fu.sep490.g23.backend.dto.response.assessment.PlacementRecommendationResponse;
import fu.sep490.g23.backend.dto.response.assessment.PlacementSkillScoresResponse;
import fu.sep490.g23.backend.dto.response.assessment.RecommendedInstructorLedCourseResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.course.InstructorLedCourseRepository;
import fu.sep490.g23.backend.service.assessment.PlacementEligibilityService;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationContext;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationContextFactory;
import fu.sep490.g23.backend.service.assessment.PlacementRecommendationService;
import fu.sep490.g23.backend.service.course.LearningPathRecommendationService;
import fu.sep490.g23.backend.service.course.OnlineCourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Builds course / training-program / learning-path suggestions from a scored attempt.
 * Scores, weak skills, and recommended level come from PlacementRecommendationContext.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlacementRecommendationServiceImpl implements PlacementRecommendationService {
    private static final Pattern SCORE_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");
    private static final Pattern IELTS_SCORE_PATTERN = Pattern.compile("IELTS\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern TOEIC_SCORE_PATTERN = Pattern.compile("TOEIC\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);

    private final UserRepository userRepository;
    private final PlacementTestAttemptRepository attemptRepository;
    private final PlacementEligibilityService eligibilityService;
    private final PlacementRecommendationContextFactory contextFactory;
    private final OnlineCourseService onlineCourseService;
    private final InstructorLedCourseRepository instructorLedCourseRepository;
    private final LearningPathRecommendationService learningPathRecommendationService;

    /**
     * Main recommendation entry. Nested calls, in order:
     * 1. evaluateEligibility — can this attempt be used for placement?
     * 2. fromAttempt — pack scores + weak skills + learner target
     * 3. baseResponse — always return scores/status even if lists are empty
     * 4. canBuildRecommendations — skip ranking if expired / skill-only / no overall
     * 5. recommendCourses / recommendInstructorLedCourses / learning-path recommend
     */
    @Override
    public PlacementRecommendationResponse getRecommendations(Long attemptId, String learnerEmail) {
        User learner = userRepository.findByEmail(learnerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        PlacementTestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả placement test."));
        // 1) Completeness / expiry / staff-review status for this attempt.
        PlacementEligibilityResult eligibility = eligibilityService.evaluateEligibility(learner.getId(), attemptId);
        // 2) Ranking input: scores, exam type, weak skills, learner target.
        PlacementRecommendationContext context = contextFactory.fromAttempt(
                learner,
                attempt,
                eligibility.getRecommendedLevel()
        );

        // 3) Always include scores + status, even when suggestion lists stay empty.
        PlacementRecommendationResponse.PlacementRecommendationResponseBuilder response = baseResponse(
                attempt,
                eligibility,
                context
        );
        if (!canBuildRecommendations(attempt, eligibility)) {
            // 4) Not ready to rank products: keep scores, return empty suggestion lists.
            return response
                    .recommendationReady(false)
                    .message(readinessMessage(eligibility.getStatus()))
                    .recommendedOnlineCourses(List.of())
                    .recommendedInstructorLedCourses(List.of())
                    .recommendedLearningPath(null)
                    .build();
        }

        // 5) Rank online courses, classroom programs, and pick one learning path.
        return response
                .recommendationReady(true)
                .message(null)
                .recommendedOnlineCourses(onlineCourseService.recommendCourses(learner, context))
                .recommendedInstructorLedCourses(recommendInstructorLedCourses(context))
                .recommendedLearningPath(learningPathRecommendationService.recommend(learner, context, true))
                .build();
    }

    /**
     * Gate before ranking.
     * true = ELIGIBLE, or IELTS still waiting for staff but already has an overall score.
     */
    private boolean canBuildRecommendations(
            PlacementTestAttempt attempt,
            PlacementEligibilityResult eligibility
    ) {
        if (eligibility.isEligible()) return true;
        PlacementEvaluationStatus status = eligibility.getStatus();
        boolean awaitingReview = status == PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED
                || status == PlacementEvaluationStatus.UNDER_REVIEW;
        return awaitingReview && attempt.getOverallScore() != null;
    }

    /** Fill scores, status, weak skills, and whether the learner has a target score. */
    private PlacementRecommendationResponse.PlacementRecommendationResponseBuilder baseResponse(
            PlacementTestAttempt attempt,
            PlacementEligibilityResult eligibility,
            PlacementRecommendationContext context
    ) {
        return PlacementRecommendationResponse.builder()
                .attemptId(attempt.getId())
                .examType(context.getExamType())
                .evaluationStatus(eligibility.getStatus())
                .overallScore(attempt.getOverallScore())
                .recommendedLevel(eligibility.getRecommendedLevel())
                .skillScores(PlacementSkillScoresResponse.builder()
                        .listening(attempt.getListeningScore())
                        .reading(attempt.getReadingScore())
                        .writing(attempt.getWritingScore())
                        .speaking(attempt.getSpeakingScore())
                        .build())
                .weakSkills(context.getWeakSkills().stream().map(Enum::name).toList())
                .targetMissing(context.getTargetScore() == null);
    }

    /**
     * Core ranking for placement to instructor-led courses.
     *
     * Pipeline:
     * 1. Load active instructor-led courses.
     * 2. Keep PUBLISHED product + PUBLISHED curriculum only.
     * 3. Same exam category as placement (IELTS vs TOEIC) — hard filter, unlike online courses.
     * 4. Keep courses whose numeric entry/target range contains the learner's score.
     * 5. instructorLedCourseScore() - entry proximity, weak skills, and target fit.
     * 6. Sort by score desc; tie-break by id.
     * 7. Keep 6; toInstructorLedCourseResponse() adds the Vietnamese reason.
     */
    private List<RecommendedInstructorLedCourseResponse> recommendInstructorLedCourses(PlacementRecommendationContext context) {
        return instructorLedCourseRepository.findAllByOrderByUpdatedAtDescIdDesc().stream()
                .filter(program -> program.getPublicationStatus() == PackageStatus.PUBLISHED)
                .filter(program -> context.getExamType().equalsIgnoreCase(program.getExamType()))
                .filter(program -> isScoreEligible(program, context))
                .map(program -> new ScoredInstructorLedCourse(program, instructorLedCourseScore(program, context)))
                .sorted(Comparator.comparingDouble(ScoredInstructorLedCourse::score).reversed()
                        .thenComparing(item -> item.program().getId()))
                .limit(6)
                .map(item -> toInstructorLedCourseResponse(item.program(), context))
                .toList();
    }

    /**
     * Score one instructor-led course. Start at 20 so a mild mismatch still ranks above zero.
     *
     *   +0..15    learner score is close to the course's numeric entry score
     *   +8 each   focus skill overlaps a placement weak skill
     *   +5        learner is inside the course's entry-to-target growth range
     *   +3        course target still covers the learner's personal goal
     *
     * IELTS uses targetBand; TOEIC uses targetScore.
     */
    private double instructorLedCourseScore(InstructorLedCourse program, PlacementRecommendationContext context) {
        double score = 20;
        BigDecimal entry = entryScore(program);
        BigDecimal target = targetScore(program);
        BigDecimal current = context.getOverallScore();

        if (entry != null && target != null && current != null) {
            BigDecimal range = target.subtract(entry);
            if (range.compareTo(BigDecimal.ZERO) > 0) {
                double progress = current.subtract(entry)
                        .divide(range, 6, java.math.RoundingMode.HALF_UP)
                        .doubleValue();
                score += Math.max(0D, 15D * (1D - Math.min(1D, progress)));
            }
            score += 5;
        }

        Set<AssessmentSkill> focusSkills = focusSkills(program.getFocusSkills());
        score += focusSkills.stream().filter(context.getWeakSkills()::contains).count() * 8D;
        if (target != null && context.getTargetScore() != null
                && target.compareTo(context.getTargetScore()) >= 0) {
            score += 3;
        }
        return score;
    }

    /**
     * Map a ranked program to the API DTO.
     * The reason exposes the score range that made the course eligible.
     */
    private RecommendedInstructorLedCourseResponse toInstructorLedCourseResponse(
            InstructorLedCourse program,
            PlacementRecommendationContext context
    ) {
        Set<AssessmentSkill> matches = focusSkills(program.getFocusSkills()).stream()
                .filter(context.getWeakSkills()::contains)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        String reason = scoreFitReason(program, context, matches);
        return RecommendedInstructorLedCourseResponse.builder()
                .id(program.getId())
                .title(program.getTitle())
                .shortDescription(program.getShortDescription())
                .entryLevel(program.getEntryLevel())
                .examCategory(program.getExamType())
                .focusSkills(focusSkills(program.getFocusSkills()).stream().map(Enum::name).toList())
                .targetBand(program.getTargetBand())
                .targetScore(program.getTargetScore())
                .totalSessions(program.getUnits().stream().mapToInt(unit -> unit.getLessons().size()).sum())
                .price(program.getBaseTuitionFeeVnd())
                .salePrice(program.getSaleTuitionFeeVnd())
                .recommendationReason(reason)
                .build();
    }

    private boolean isScoreEligible(InstructorLedCourse program, PlacementRecommendationContext context) {
        BigDecimal current = context.getOverallScore();
        BigDecimal entry = entryScore(program);
        BigDecimal target = targetScore(program);
        return current != null
                && entry != null
                && target != null
                && entry.compareTo(target) < 0
                && current.compareTo(entry) >= 0
                && current.compareTo(target) < 0;
    }

    /** Parse legacy display values such as "IELTS 5.0" and "TOEIC 350+". */
    private BigDecimal entryScore(InstructorLedCourse program) {
        if (program == null || program.getEntryLevel() == null || program.getEntryLevel().isBlank()) {
            return null;
        }
        Matcher matcher = labeledEntryScorePattern(program).matcher(program.getEntryLevel());
        if (!matcher.find()) {
            matcher = SCORE_PATTERN.matcher(program.getEntryLevel());
            if (!matcher.find()) {
                return null;
            }
        }
        try {
            BigDecimal value = new BigDecimal(matcher.group(1));
            if ("IELTS".equalsIgnoreCase(program.getExamType())) {
                return isValidIeltsBand(value) ? value : null;
            }
            if ("TOEIC".equalsIgnoreCase(program.getExamType())) {
                return isValidToeicScore(value) ? value : null;
            }
        } catch (NumberFormatException ignored) {
            // Invalid legacy values are excluded from recommendations instead of breaking the request.
        }
        return null;
    }

    private Pattern labeledEntryScorePattern(InstructorLedCourse program) {
        return "TOEIC".equalsIgnoreCase(program.getExamType()) ? TOEIC_SCORE_PATTERN : IELTS_SCORE_PATTERN;
    }

    private BigDecimal targetScore(InstructorLedCourse program) {
        if ("IELTS".equalsIgnoreCase(program.getExamType())) {
            return program.getTargetBand();
        }
        if ("TOEIC".equalsIgnoreCase(program.getExamType()) && program.getTargetScore() != null) {
            return BigDecimal.valueOf(program.getTargetScore());
        }
        return null;
    }

    private boolean isValidIeltsBand(BigDecimal value) {
        return value.compareTo(BigDecimal.ZERO) >= 0
                && value.compareTo(BigDecimal.valueOf(9)) <= 0
                && value.multiply(BigDecimal.valueOf(2)).stripTrailingZeros().scale() <= 0;
    }

    private boolean isValidToeicScore(BigDecimal value) {
        try {
            int score = value.intValueExact();
            return score >= 10 && score <= 990 && score % 5 == 0;
        } catch (ArithmeticException ignored) {
            return false;
        }
    }

    private String scoreFitReason(
            InstructorLedCourse program,
            PlacementRecommendationContext context,
            Set<AssessmentSkill> matches
    ) {
        String scoreLabel = "IELTS".equalsIgnoreCase(program.getExamType()) ? "Band" : "Điểm";
        String reason = scoreLabel + " hiện tại " + formatScore(context.getOverallScore())
                + " phù hợp đầu vào " + formatScore(entryScore(program))
                + "; khóa hướng tới " + formatScore(targetScore(program)) + ".";
        if (!matches.isEmpty()) {
            reason += " Đồng thời tập trung vào " + skillLabel(matches.iterator().next())
                    + ", kỹ năng bạn đang cần ưu tiên.";
        }
        return reason;
    }

    private String formatScore(BigDecimal value) {
        return value == null ? "chưa xác định" : value.stripTrailingZeros().toPlainString();
    }

    /** Split curriculum focusSkills text ("LISTENING,WRITING") into enums; ignore unknown tokens. */
    private Set<AssessmentSkill> focusSkills(String value) {
        if (value == null || value.isBlank()) return Set.of();
        Set<AssessmentSkill> result = new LinkedHashSet<>();
        Arrays.stream(value.split("[,;|]"))
                .map(String::trim)
                .map(item -> item.toUpperCase(Locale.ROOT))
                .forEach(item -> {
                    try {
                        result.add(AssessmentSkill.valueOf(item));
                    } catch (IllegalArgumentException ignored) {
                        // Legacy free text is display-only; skip it for matching.
                    }
                });
        return result;
    }

    /** Message shown when recommendationReady is false. */
    private String readinessMessage(PlacementEvaluationStatus status) {
        if (status == PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED || status == PlacementEvaluationStatus.UNDER_REVIEW) {
            return "Kết quả IELTS đang chờ nhân viên đào tạo xác nhận trước khi xây dựng lộ trình phù hợp.";
        }
        if (status == PlacementEvaluationStatus.EXPIRED) return "Kết quả placement test đã hết hiệu lực.";
        if (status == PlacementEvaluationStatus.NOT_ELIGIBLE) return "Kết quả placement test chưa đủ điều kiện để gợi ý lộ trình.";
        return "Kết quả placement test chưa sẵn sàng để gợi ý lộ trình.";
    }

    /** English skill name used inside the Vietnamese reason string. */
    private String skillLabel(AssessmentSkill skill) {
        return switch (skill) {
            case LISTENING -> "Listening";
            case READING -> "Reading";
            case WRITING -> "Writing";
            case SPEAKING -> "Speaking";
            default -> skill.name();
        };
    }

    /** Temporary holder while sorting instructor-led courses by match score. */
    private record ScoredInstructorLedCourse(InstructorLedCourse program, double score) {}
}
