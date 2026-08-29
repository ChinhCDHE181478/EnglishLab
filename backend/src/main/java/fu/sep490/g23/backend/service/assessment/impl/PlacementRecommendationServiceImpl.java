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
     * 1. Load active instructor-led courses.
     * 2. Keep PUBLISHED product + PUBLISHED curriculum only.
     * 3. Same exam category as placement (IELTS vs TOEIC) — hard filter, unlike online courses.
     * 4. trainingProgramScore() — numeric match (level, weak skills, target stretch).
     * 5. Sort by score desc; tie-break by id.
     * 6. Keep 6;


    /**
     * Score one instructor-led course. Start at 20 so a mild mismatch still ranks above zero.
     *
     *   +15 / -5  entryPlacementLevel equals / differs from recommendedLevel
     *   +8 each   focus skill overlaps a placement weak skill
     *   +5        course target is above current score (room to grow)
     *   +3        course target still covers the learner's personal goal
     *
     * IELTS uses targetBand;



    /** Split curriculum focusSkills text ("LISTENING,WRITING") into enums;



    /** Temporary holder while sorting instructor-led courses by match score. */
    private record ScoredTrainingProgram(InstructorLedCourse program, double score) {}
}
