package fu.sep490.g23.backend.service.assessment.impl;

import fu.sep490.g23.backend.dto.response.assessment.PlacementEligibilityResult;
import fu.sep490.g23.backend.dto.response.assessment.PlacementRecommendationResponse;
import fu.sep490.g23.backend.dto.response.assessment.PlacementSkillScoresResponse;
import fu.sep490.g23.backend.dto.response.assessment.RecommendedTrainingProgramResponse;
import fu.sep490.g23.backend.entity.User;
import fu.sep490.g23.backend.entity.assessment.PlacementTestAttempt;
import fu.sep490.g23.backend.entity.assessment.enums.AssessmentSkill;
import fu.sep490.g23.backend.entity.assessment.enums.PlacementEvaluationStatus;
import fu.sep490.g23.backend.entity.classroom.TrainingProgram;
import fu.sep490.g23.backend.entity.course.enums.PackageStatus;
import fu.sep490.g23.backend.entity.curriculum.CurriculumProgram;
import fu.sep490.g23.backend.repository.UserRepository;
import fu.sep490.g23.backend.repository.assessment.PlacementTestAttemptRepository;
import fu.sep490.g23.backend.repository.classroom.TrainingProgramRepository;
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

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlacementRecommendationServiceImpl implements PlacementRecommendationService {
    private final UserRepository userRepository;
    private final PlacementTestAttemptRepository attemptRepository;
    private final PlacementEligibilityService eligibilityService;
    private final PlacementRecommendationContextFactory contextFactory;
    private final OnlineCourseService onlineCourseService;
    private final TrainingProgramRepository trainingProgramRepository;
    private final LearningPathRecommendationService learningPathRecommendationService;

    @Override
    public PlacementRecommendationResponse getRecommendations(Long attemptId, String learnerEmail) {
        User learner = userRepository.findByEmail(learnerEmail)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học viên."));
        PlacementTestAttempt attempt = attemptRepository.findById(attemptId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy kết quả placement test."));
        PlacementEligibilityResult eligibility = eligibilityService.evaluateEligibility(learner.getId(), attemptId);
        PlacementRecommendationContext context = contextFactory.fromAttempt(
                learner,
                attempt,
                eligibility.getRecommendedLevel()
        );

        PlacementRecommendationResponse.PlacementRecommendationResponseBuilder response = baseResponse(
                attempt,
                eligibility,
                context
        );
        if (!canBuildRecommendations(attempt, eligibility)) {
            return response
                    .recommendationReady(false)
                    .message(readinessMessage(eligibility.getStatus()))
                    .recommendedOnlineCourses(List.of())
                    .recommendedTrainingPrograms(List.of())
                    .recommendedLearningPath(null)
                    .build();
        }

        return response
                .recommendationReady(true)
                .message(null)
                .recommendedOnlineCourses(onlineCourseService.recommendCourses(learner, context))
                .recommendedTrainingPrograms(recommendTrainingPrograms(context))
                .recommendedLearningPath(learningPathRecommendationService.recommend(learner, context, true))
                .build();
    }

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

    private List<RecommendedTrainingProgramResponse> recommendTrainingPrograms(PlacementRecommendationContext context) {
        return trainingProgramRepository.findAllByOrderByDisplayOrderAscUpdatedAtDescIdDesc().stream()
                .filter(program -> program.getStatus() == PackageStatus.PUBLISHED)
                .filter(program -> program.getCurriculumProgram() != null
                        && "PUBLISHED".equalsIgnoreCase(program.getCurriculumProgram().getStatus()))
                .filter(program -> context.getExamType().equalsIgnoreCase(program.getCurriculumProgram().getExamCategory()))
                .map(program -> new ScoredTrainingProgram(program, trainingProgramScore(program, context)))
                .sorted(Comparator.comparingDouble(ScoredTrainingProgram::score).reversed()
                        .thenComparing(item -> item.program().getDisplayOrder())
                        .thenComparing(item -> item.program().getId()))
                .limit(6)
                .map(item -> toTrainingResponse(item.program(), context))
                .toList();
    }

    private double trainingProgramScore(TrainingProgram program, PlacementRecommendationContext context) {
        CurriculumProgram curriculum = program.getCurriculumProgram();
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

    private RecommendedTrainingProgramResponse toTrainingResponse(
            TrainingProgram program,
            PlacementRecommendationContext context
    ) {
        CurriculumProgram curriculum = program.getCurriculumProgram();
        Set<AssessmentSkill> matches = focusSkills(curriculum.getFocusSkills()).stream()
                .filter(context.getWeakSkills()::contains)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        String reason = !matches.isEmpty()
                ? "Tập trung vào " + skillLabel(matches.iterator().next()) + ", kỹ năng bạn đang cần ưu tiên."
                : curriculum.getEntryPlacementLevel() == context.getRecommendedLevel()
                    ? "Phù hợp với trình độ " + levelLabel(context.getRecommendedLevel().name()) + " hiện tại của bạn."
                    : "Phù hợp với mục tiêu " + context.getExamType() + " của bạn.";
        return RecommendedTrainingProgramResponse.builder()
                .id(program.getId())
                .slug(program.getSlug())
                .title(program.getTitle())
                .deliveryMode(program.getDeliveryMode())
                .thumbnailUrl(program.getThumbnailUrl())
                .shortDescription(program.getShortDescription())
                .entryPlacementLevel(curriculum.getEntryPlacementLevel())
                .examCategory(curriculum.getExamCategory())
                .programTrack(curriculum.getProgramTrack())
                .focusSkills(focusSkills(curriculum.getFocusSkills()).stream().map(Enum::name).toList())
                .targetBand(curriculum.getTargetBand())
                .targetScore(curriculum.getTargetScore())
                .totalSessions(curriculum.getTotalSessions())
                .price(program.getPrice())
                .salePrice(program.getSalePrice())
                .recommendationReason(reason)
                .build();
    }

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
                        // Legacy text remains display-only and is ignored for structured matching.
                    }
                });
        return result;
    }

    private String readinessMessage(PlacementEvaluationStatus status) {
        if (status == PlacementEvaluationStatus.MANUAL_REVIEW_REQUIRED || status == PlacementEvaluationStatus.UNDER_REVIEW) {
            return "Kết quả IELTS đang chờ nhân viên đào tạo xác nhận trước khi xây dựng lộ trình phù hợp.";
        }
        if (status == PlacementEvaluationStatus.EXPIRED) return "Kết quả placement test đã hết hiệu lực.";
        if (status == PlacementEvaluationStatus.NOT_ELIGIBLE) return "Kết quả placement test chưa đủ điều kiện để gợi ý lộ trình.";
        return "Kết quả placement test chưa sẵn sàng để gợi ý lộ trình.";
    }

    private String skillLabel(AssessmentSkill skill) {
        return switch (skill) {
            case LISTENING -> "Listening";
            case READING -> "Reading";
            case WRITING -> "Writing";
            case SPEAKING -> "Speaking";
            default -> skill.name();
        };
    }

    private String levelLabel(String level) {
        return switch (level) {
            case "BEGINNER" -> "Cơ bản";
            case "INTERMEDIATE" -> "Trung cấp";
            case "ADVANCED" -> "Nâng cao";
            default -> level;
        };
    }

    private record ScoredTrainingProgram(TrainingProgram program, double score) {}
}
