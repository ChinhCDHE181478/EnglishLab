package fu.sep490.g23.backend.service.assessment.impl;

import fu.sep490.g23.backend.dto.response.assessment.PlacementEligibilityResult;
import fu.sep490.g23.backend.dto.response.assessment.PlacementRecommendationResponse;
import fu.sep490.g23.backend.dto.response.assessment.PlacementSkillScoresResponse;
import fu.sep490.g23.backend.dto.response.assessment.RecommendedTrainingProgramResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.course.InstructorLedCourse;
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

/**
 * Builds course / training-program / learning-path suggestions from a scored attempt.
 * Scores, weak skills, and recommended level come from PlacementRecommendationContext.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlacementRecommendationServiceImpl implements PlacementRecommendationService {
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
     * 5. recommendCourses / recommendTrainingPrograms / learning-path recommend
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
                    .recommendedTrainingPrograms(List.of())
                    .recommendedLearningPath(null)
                    .build();
        }

        // 5) Rank online courses, classroom programs, and pick one learning path.
        return response
                .recommendationReady(true)
                .message(null)
                .recommendedOnlineCourses(onlineCourseService.recommendCourses(learner, context))
                .recommendedTrainingPrograms(recommendTrainingPrograms(context))
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
     * Core ranking for placement → classroom training programs.
     *
     * Pipeline:
     * 1. Load programs (already ordered by displayOrder).
     * 2. Keep PUBLISHED product + PUBLISHED curriculum only.
     * 3. Same exam category as placement (IELTS vs TOEIC) — hard filter, unlike online courses.
     * 4. trainingProgramScore() — numeric match (level, weak skills, target stretch).
     * 5. Sort by score desc; tie-break displayOrder then id.
     * 6. Keep 6; toTrainingResponse() adds the Vietnamese reason.
     */
    private List<RecommendedTrainingProgramResponse> recommendTrainingPrograms(PlacementRecommendationContext context) {
        return instructorLedCourseRepository.findAllByOrderByDisplayOrderAscUpdatedAtDescIdDesc().stream()
                .filter(program -> program.getPublicationStatus() == PackageStatus.PUBLISHED)
                .filter(program -> context.getExamType().equalsIgnoreCase(program.getExamType()))
                .map(program -> new ScoredTrainingProgram(program, trainingProgramScore(program, context)))
                .sorted(Comparator.comparingDouble(ScoredTrainingProgram::score).reversed()
                        .thenComparing(item -> item.program().getDisplayOrder())
                        .thenComparing(item -> item.program().getId()))
                .limit(6)
                .map(item -> toTrainingResponse(item.program(), context))
                .toList();
    }

    /**
     * Score one instructor-led course. Start at 20 so a mild mismatch still ranks above zero.
     *
     *   +15 / -5  entryPlacementLevel equals / differs from recommendedLevel
     *   +8 each   focus skill overlaps a placement weak skill
     *   +5        course target is above current score (room to grow)
     *   +3        course target still covers the learner's personal goal
     *   +1        course is featured
     *
     * IELTS uses targetBand; TOEIC uses targetScore.
     */
    private double trainingProgramScore(InstructorLedCourse program, PlacementRecommendationContext context) {
        InstructorLedCourse curriculum = program;
        double score = 20;
        if (curriculum.getEntryPlacementLevel() != null && context.getRecommendedLevel() != null) {
            score += curriculum.getEntryPlacementLevel() == context.getRecommendedLevel() ? 15 : -5;
        }
        Set<AssessmentSkill> focusSkills = focusSkills(curriculum.getFocusSkills());
        score += focusSkills.stream().filter(context.getWeakSkills()::contains).count() * 8D;
        if ("IELTS".equals(context.getExamType()) && context.getOverallScore() != null && curriculum.getTargetBand() != null) {
            BigDecimal target = context.getTargetScore();
            if (curriculum.getTargetBand().compareTo(context.getOverallScore()) > 0) score += 5;
            if (target != null && curriculum.getTargetBand().compareTo(target) <= 0) score += 3;
        }
        if ("TOEIC".equals(context.getExamType()) && curriculum.getTargetScore() != null) {
            if (context.getOverallScore() != null && curriculum.getTargetScore() > context.getOverallScore().intValue()) score += 5;
            if (context.getTargetScore() != null && curriculum.getTargetScore() <= context.getTargetScore().intValue()) score += 3;
        }
        if (program.isFeatured()) score += 1;
        return score;
    }

    /**
     * Map a ranked program to the API DTO.
     * Reason priority: covers a weak skill → same placement level → same exam type.
     */
    private RecommendedTrainingProgramResponse toTrainingResponse(
            InstructorLedCourse program,
            PlacementRecommendationContext context
    ) {
        InstructorLedCourse curriculum = program;
        Set<AssessmentSkill> matches = focusSkills(curriculum.getFocusSkills()).stream()
                .filter(context.getWeakSkills()::contains)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        // Reason priority: covers a weak skill > same placement level > same exam type.
        String reason = !matches.isEmpty()
                ? "Tập trung vào " + skillLabel(matches.iterator().next()) + ", kỹ năng bạn đang cần ưu tiên."
                : curriculum.getEntryPlacementLevel() == context.getRecommendedLevel()
                    ? "Phù hợp với trình độ " + levelLabel(context.getRecommendedLevel().name()) + " hiện tại của bạn."
                    : "Phù hợp với mục tiêu " + context.getExamType() + " của bạn.";
        return RecommendedTrainingProgramResponse.builder()
                .id(program.getId())
                .slug(program.getSlug())
                .title(program.getTitle())
                .deliveryMode(null)
                .thumbnailUrl(program.getThumbnailUrl())
                .shortDescription(program.getShortDescription())
                .entryPlacementLevel(curriculum.getEntryPlacementLevel())
                .examCategory(curriculum.getExamType())
                .programTrack(curriculum.getProgramTrack())
                .focusSkills(focusSkills(curriculum.getFocusSkills()).stream().map(Enum::name).toList())
                .targetBand(curriculum.getTargetBand())
                .targetScore(curriculum.getTargetScore())
                .totalSessions(curriculum.getUnits().stream().mapToInt(unit -> unit.getLessons().size()).sum())
                .price(program.getBaseTuitionFeeVnd())
                .salePrice(program.getSaleTuitionFeeVnd())
                .recommendationReason(reason)
                .build();
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

    /** Vietnamese label for BEGINNER / INTERMEDIATE / ADVANCED. */
    private String levelLabel(String level) {
        return switch (level) {
            case "BEGINNER" -> "Cơ bản";
            case "INTERMEDIATE" -> "Trung cấp";
            case "ADVANCED" -> "Nâng cao";
            default -> level;
        };
    }

    /** Temporary holder while sorting instructor-led courses by match score. */
    private record ScoredTrainingProgram(InstructorLedCourse program, double score) {}
}
